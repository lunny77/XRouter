package com.xrouter;

public class RouteMeta {
    String target;
    String path;
    String group;

    private RouteMeta(String target, String path,String group) {
        this.target = target;
        this.path = path;
        this.group = group;
    }

    public static RouteMeta create(String target, String path, String group) {
        return new RouteMeta(target,path, group);
    }
}
