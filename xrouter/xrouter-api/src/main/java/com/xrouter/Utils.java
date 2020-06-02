package com.xrouter;

import java.util.Objects;

public class Utils {

    public static String getGroupFromPath(String path) {
        Objects.requireNonNull(path);
        return path.substring(path.indexOf("/") + 1, path.lastIndexOf("/"));
    }

}
