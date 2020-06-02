package com.xrouter.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;
import com.xrouter.annotation.Route;
import com.xrouter.annotation.Interceptor;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

public class XRouterProcessor extends AbstractProcessor {
    private Messager mMessager;
    private Filer mFiler;

    private final Set<String> supportedAnnotationTypes = new HashSet<>(Arrays.asList(
            Route.class.getName(),
            Interceptor.class.getName()
    ));

    private final String KEY_MODULE_NAME = "XROUTER_MODULE_NAME";
    private String moduleName;


    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mMessager = processingEnv.getMessager();
        mFiler = processingEnv.getFiler();
        Map<String, String> options = processingEnv.getOptions();
        mMessager.printMessage(Diagnostic.Kind.NOTE, "[XRouter] init(),  opts:" + options);
        moduleName = options.get(KEY_MODULE_NAME);
        mMessager.printMessage(Diagnostic.Kind.NOTE, "[XRouter] handle module: " + moduleName);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) return false;
        mMessager.printMessage(Diagnostic.Kind.NOTE, "[XRouter] process()");
        if (annotations != null) {
            annotations.forEach(typeElement -> {
                mMessager.printMessage(Diagnostic.Kind.NOTE, "[XRouter] annotation: " + typeElement.toString());
                List<? extends Element> enclosedElements = typeElement.getEnclosedElements();
                enclosedElements.forEach(element -> {
                    mMessager.printMessage(Diagnostic.Kind.NOTE, "[XRouter] enclosed element: " +
                            element.getSimpleName() + " || " + element.asType().toString());
                });

                Element enclosingElement = typeElement.getEnclosingElement();
                mMessager.printMessage(Diagnostic.Kind.NOTE, "[XRouter] enclosing element: " + enclosingElement.getSimpleName() +
                        " || " + enclosingElement.asType().toString());

                Set<? extends Element> elementsAnnotatedWith = roundEnv.getElementsAnnotatedWith(typeElement);
                for (Element element : elementsAnnotatedWith) {
                    Route annotation = element.getAnnotation(Route.class);
                    Annotation[] declaredAnnotations = annotation.annotationType().getDeclaredAnnotations();
                    for (Annotation declaredAnnotation : declaredAnnotations) {
                        mMessager.printMessage(Diagnostic.Kind.NOTE, "[XRouter] declaredAnnotation " + declaredAnnotation.toString());
                    }
                    break;
                }
            });
        }

        if (annotations == null || annotations.isEmpty()) {
            return false;
        }

        Set<? extends Element> routeElements = roundEnv.getElementsAnnotatedWith(Route.class);

        //生成XRouter_Group_<groupName>文件
        generateGroup(routeElements);

        //生成XRouter_Group_<moduleName>文件
        generateRoot(routeElements);

        return false;
    }

    private void generateGroup(Set<? extends Element> elements) {
        if (elements != null) {
            //泛型参数 Map<String, RouteMeta>
            ParameterizedTypeName parameterizedTypeName = ParameterizedTypeName.get(ClassName.get(Map.class),
                    TypeName.get(String.class), Constants.ROUTE_META_CLASS_NAME);

            //对path进行分组
            Map<String, Set<Element>> groupMap = new HashMap<>();
            elements.forEach(element -> {
                String path = element.getAnnotation(Route.class).path();
                String groupFromPath = getGroupFromPath(path);
                if (!groupMap.containsKey(groupFromPath)) {
                    groupMap.put(groupFromPath, new HashSet<>());
                }

                Set<Element> pathSet = groupMap.get(groupFromPath);
                pathSet.add(element);
            });

            groupMap.forEach((group, pathSet) -> {
                //构造方法 void loadInto(Map<String, RouteMeta> atlas)
                MethodSpec.Builder loadIntoBuilder = MethodSpec.methodBuilder("loadInto")
                        .addAnnotation(Override.class)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(TypeName.VOID)
                        .addParameter(parameterizedTypeName, "atlas");

                //构造方法 XRouter_Group_<groupName>类
                TypeSpec.Builder groupClassBuilder = TypeSpec.classBuilder(Constants.ROUTE_GROUP_PREFIX + group)
                        .addModifiers(Modifier.PUBLIC)
                        .addSuperinterface(Constants.ROUTE_GROUP_ClASS_NAME)
                        .addJavadoc(Constants.JAVA_DOC);

                pathSet.forEach(element -> {
                    String path = element.getAnnotation(Route.class).path();
                    loadIntoBuilder.addStatement("atlas.put($S, $T.create($S, $S, $S))", path, Constants.ROUTE_META_CLASS_NAME,
                            element.asType().toString(), path, group);
                });

                TypeSpec groupClass = groupClassBuilder.addMethod(loadIntoBuilder.build()).build();
                try {
                    JavaFile.builder(Constants.PACKAGE_OF_GENERATED_CODE, groupClass).build().writeTo(mFiler);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void generateRoot(Set<? extends Element> elements) {
        if (elements != null) {
            //泛型参数 Map<String, Class<? extends IRouteGroup>>
            ParameterizedTypeName iRouteRootParameter = ParameterizedTypeName.get(ClassName.get(Map.class),
                    ClassName.get(String.class),
                    ParameterizedTypeName.get(ClassName.get(Class.class),
                            WildcardTypeName.subtypeOf(Constants.ROUTE_GROUP_ClASS_NAME)));

            //构造方法 void loadInto(Map<String, Class<? extends IRouteGroup>> roots)
            MethodSpec.Builder loadIntoBuilder = MethodSpec.methodBuilder("loadInto")
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(iRouteRootParameter, "roots")
                    .returns(TypeName.VOID);

            //对path进行分组
            Set<String> groupSet = new HashSet<>();
            elements.forEach((Consumer<Element>) element -> {
                String path = element.getAnnotation(Route.class).path();
                String groupFromPath = getGroupFromPath(path);
                if (!groupSet.contains(groupFromPath)) {
                    groupSet.add(groupFromPath);
                }
            });

            groupSet.forEach(group -> loadIntoBuilder.addStatement("roots.put($S, $T.class)", group,
                    ClassName.get("com.xrouter.routes", Constants.ROUTE_GROUP_PREFIX + group)));

            //构造方法 XRouter_Root_<moduleName>类
            TypeSpec rootType = TypeSpec.classBuilder("XRouter_Root_" + moduleName)
                    .addModifiers(Modifier.PUBLIC)
                    .addSuperinterface(Constants.ROUTE_ROOT_ClASS_NAME)
                    .addMethod(loadIntoBuilder.build())
                    .addJavadoc(" DO NOT EDIT THIS FILE!!! IT WAS GENERATED BY XROUTER. ")
                    .build();

            try {
                JavaFile.builder("com.xrouter.routes", rootType).build().writeTo(mFiler);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        mMessager.printMessage(Diagnostic.Kind.NOTE, "[XRouter] getSupportedAnnotationTypes()");
        return supportedAnnotationTypes;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        mMessager.printMessage(Diagnostic.Kind.NOTE, "[XRouter] getSupportedSourceVersion()");
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedOptions() {
        mMessager.printMessage(Diagnostic.Kind.NOTE, "[XRouter] getSupportedOptions()");
        return super.getSupportedOptions();
    }

    public String getGroupFromPath(String path) {
        Objects.requireNonNull(path);
        return path.substring(path.indexOf("/") + 1, path.lastIndexOf("/"));
    }
}
