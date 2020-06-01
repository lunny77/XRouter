package com.xrouter.compiler;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.xrouter.annotation.Route;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
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
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.swing.Action;
import javax.tools.Diagnostic;

public class XRouterProcessor extends AbstractProcessor {
    private Messager mMessager;
    private Filer mFiler;
    private final Set<String> supportedAnnotationTypes = new HashSet<>(Arrays.asList(
            Route.class.getName()
    ));

    private final String KEY_MODULE_NAME = "XROUTER_MODULE_NAME";
    private String moduleName;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mMessager = processingEnv.getMessager();
        mFiler = processingEnv.getFiler();
        mMessager.printMessage(Diagnostic.Kind.NOTE, "[XRouter] init()");

        Map<String, String> options = processingEnv.getOptions();
        mMessager.printMessage(Diagnostic.Kind.NOTE, "[XRouter] opts:" + options.toString());
        moduleName = options.get(KEY_MODULE_NAME);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) return false;
        mMessager.printMessage(Diagnostic.Kind.NOTE, "[XRouter] process()");
        if (annotations != null) {
            annotations.forEach((Consumer<TypeElement>) typeElement -> {
                mMessager.printMessage(Diagnostic.Kind.NOTE, "[XRouter] typeElement:" + typeElement.toString());
                Set<? extends Element> elementsAnnotatedWith = roundEnv.getElementsAnnotatedWith(typeElement);
                elementsAnnotatedWith.forEach(element -> {
                    mMessager.printMessage(Diagnostic.Kind.NOTE, "[XRouter] -->:" + element.getSimpleName());
                });
            });
        }

        if (annotations == null || annotations.isEmpty()) {
            return false;
        }

        MethodSpec.Builder initMethodBuilder = MethodSpec.methodBuilder("init")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(TypeName.VOID);

        Set<? extends Element> routeElements = roundEnv.getElementsAnnotatedWith(Route.class);
        if (routeElements != null) {
            ClassName wareHouse = ClassName.get("com.xrouter", "WareHouse");
            routeElements.forEach(element -> {
                Route route = element.getAnnotation(Route.class);
                String activityName = element.asType().toString();
                initMethodBuilder.addStatement("$T.putActivity($S, $S)", wareHouse, route.path(), activityName);
            });
        }

        TypeSpec wareHouse_utils = TypeSpec.classBuilder(ClassName.get("com.xrouter", "WareHouse_" + moduleName))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethod(initMethodBuilder.build())
                .build();

        try {
            JavaFile.builder("com.xrouter", wareHouse_utils).build().writeTo(mFiler);
        } catch (IOException e) {
            mMessager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }

        return false;
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
}
