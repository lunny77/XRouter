package com.xrouter;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dalvik.system.DexFile;

public class XRouter {
    private static Context context;

    private static final int VM_WITH_MULTIDEX_VERSION_MAJOR = 2;
    private static final int VM_WITH_MULTIDEX_VERSION_MINOR = 1;

    public static void init(@NonNull Application application) {
        Objects.requireNonNull(application);
        context = application.getApplicationContext();

        try {
            DexFile dexFile = DexFile.loadDex(application.getPackageCodePath(),
                    application.getCacheDir().toString(), 0);
            Log.d("XRouter", dexFile.getName());
            Enumeration<String> entries = dexFile.entries();
            while (entries.hasMoreElements()) {
                String s = entries.nextElement();
                if (s.startsWith("com.xrouter.WareHouse_")) {
                    Log.d("XRouter", "entry: " + s);
                    Class<?> aClass = Class.forName(s);
                    Method method = aClass.getDeclaredMethod("init");
                    method.invoke(null);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        Log.d("XRouter", "WareHouse map:" + WareHouse.activityMap.toString());
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public static List<String> getSourcePaths(Context context) throws PackageManager.NameNotFoundException, IOException {
        ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);

        List<String> strings = tryLoadInstantRunDexFile(applicationInfo);
        if (strings != null) {
            for (String string : strings) {
                Log.d("XRouter", "getSourcePaths()    instant run dex dir:" + string);
            }
        }

        Log.d("XRouter", "getSourcePaths()   sourceDir:" + applicationInfo.sourceDir);
        File sourceApk = new File(applicationInfo.sourceDir);
        Log.d("XRouter", "getSourcePaths()   sourceApk: " + sourceApk.getName());

        String[] splitSourceDirs = applicationInfo.splitSourceDirs;
        if (splitSourceDirs != null) {
            for (String splitSourceDir : splitSourceDirs) {
                Log.d("XRouter", "getSourcePaths()    splitSourceDir: " + splitSourceDir);
            }
        }

        String[] splitPublicSourceDirs = applicationInfo.splitPublicSourceDirs;
        if (splitPublicSourceDirs != null) {
            for (String splitSourceDir : splitPublicSourceDirs) {
                Log.d("XRouter", "getSourcePaths()    splitPublicSourceDir: " + splitSourceDir);
            }
        }

        List<String> sourcePaths = new ArrayList<>();
        sourcePaths.add(applicationInfo.sourceDir); //add the default apk path

        //the prefix of extracted file, ie: test.classes
//        String extractedFilePrefix = sourceApk.getName() + ".classes";

//        如果VM已经支持了MultiDex，就不要去Secondary Folder加载 Classesx.zip了，那里已经么有了
//        通过是否存在sp中的multidex.version是不准确的，因为从低版本升级上来的用户，是包含这个sp配置的
//        if (!isVMMultidexCapable()) {
//            //the total dex numbers
//            int totalDexNumber = getMultiDexPreferences(context).getInt(KEY_DEX_NUMBER, 1);
//            File dexDir = new File(applicationInfo.dataDir, SECONDARY_FOLDER_NAME);
//
//            for (int secondaryNumber = 2; secondaryNumber <= totalDexNumber; secondaryNumber++) {
//                //for each dex file, ie: test.classes2.zip, test.classes3.zip...
//                String fileName = extractedFilePrefix + secondaryNumber + EXTRACTED_SUFFIX;
//                File extractedFile = new File(dexDir, fileName);
//                if (extractedFile.isFile()) {
//                    sourcePaths.add(extractedFile.getAbsolutePath());
//                    //we ignore the verify zip part
//                } else {
//                    throw new IOException("Missing extracted secondary dex file '" + extractedFile.getPath() + "'");
//                }
//            }
//        }
//
//        if (ARouter.debuggable()) { // Search instant run support only debuggable
//            sourcePaths.addAll(tryLoadInstantRunDexFile(applicationInfo));
//        }
        return sourcePaths;
    }

    private static List<String> tryLoadInstantRunDexFile(ApplicationInfo applicationInfo) {
        List<String> instantRunSourcePaths = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && null != applicationInfo.splitSourceDirs) {
            // add the split apk, normally for InstantRun, and newest version.
            instantRunSourcePaths.addAll(Arrays.asList(applicationInfo.splitSourceDirs));
            Log.d("ARouter", "Found InstantRun support");
        } else {
            try {
                // This man is reflection from Google instant run sdk, he will tell me where the dex files go.
                Class pathsByInstantRun = Class.forName("com.android.tools.fd.runtime.Paths");
                Method getDexFileDirectory = pathsByInstantRun.getMethod("getDexFileDirectory", String.class);
                String instantRunDexPath = (String) getDexFileDirectory.invoke(null, applicationInfo.packageName);

                File instantRunFilePath = new File(instantRunDexPath);
                if (instantRunFilePath.exists() && instantRunFilePath.isDirectory()) {
                    File[] dexFile = instantRunFilePath.listFiles();
                    for (File file : dexFile) {
                        if (null != file && file.exists() && file.isFile() && file.getName().endsWith(".dex")) {
                            instantRunSourcePaths.add(file.getAbsolutePath());
                        }
                    }
                    Log.d("ARouter", "Found InstantRun support");
                }

            } catch (Exception e) {
                Log.e("ARouter", "InstantRun support error, " + e.getMessage());
            }
        }

        return instantRunSourcePaths;
    }

    /**
     * Identifies if the current VM has a native support for multidex, meaning there is no need for
     * additional installation by this library.
     *
     * @return true if the VM handles multidex
     */
    private static boolean isVMMultidexCapable() {
        boolean isMultidexCapable = false;
        String vmName = null;

        try {
            if (isYunOS()) {    // YunOS需要特殊判断
                vmName = "'YunOS'";
                isMultidexCapable = Integer.valueOf(System.getProperty("ro.build.version.sdk")) >= 21;
            } else {    // 非YunOS原生Android
                vmName = "'Android'";
                String versionString = System.getProperty("java.vm.version");
                if (versionString != null) {
                    Matcher matcher = Pattern.compile("(\\d+)\\.(\\d+)(\\.\\d+)?").matcher(versionString);
                    if (matcher.matches()) {
                        try {
                            int major = Integer.parseInt(matcher.group(1));
                            int minor = Integer.parseInt(matcher.group(2));
                            isMultidexCapable = (major > VM_WITH_MULTIDEX_VERSION_MAJOR)
                                    || ((major == VM_WITH_MULTIDEX_VERSION_MAJOR)
                                    && (minor >= VM_WITH_MULTIDEX_VERSION_MINOR));
                        } catch (NumberFormatException ignore) {
                            // let isMultidexCapable be false
                        }
                    }
                }
            }
        } catch (Exception ignore) {

        }

        Log.i("XRouter", "VM with name " + vmName + (isMultidexCapable ? " has multidex support" : " does not have multidex support"));
        return isMultidexCapable;
    }

    /**
     * 判断系统是否为YunOS系统
     */
    private static boolean isYunOS() {
        try {
            String version = System.getProperty("ro.yunos.version");
            String vmName = System.getProperty("java.vm.name");
            return (vmName != null && vmName.toLowerCase().contains("lemur"))
                    || (version != null && version.trim().length() > 0);
        } catch (Exception ignore) {
            return false;
        }
    }

    public static void navigate(String path) {
        String activityName = WareHouse.activityMap.get(path);
        if (activityName == null) {
            Toast.makeText(context, "Cannot find route for path:" + path, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(context.getPackageName(), activityName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
