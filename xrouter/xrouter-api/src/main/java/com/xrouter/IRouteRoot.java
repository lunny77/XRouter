package com.xrouter;

import java.util.Map;

public interface IRouteRoot {

    void loadInto(Map<String, Class<? extends IRouteGroup>> roots);

}
