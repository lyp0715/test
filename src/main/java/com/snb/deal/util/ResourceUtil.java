package com.snb.deal.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author wangshichun
 * @Description 加载资源文件
 * @Date 2018/3/6 20:32
 */
public class ResourceUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceUtil.class);

    public static InputStream getResourceInputStream(String location) throws IOException {
        Resource resource = new DefaultResourceLoader().getResource(location);
        LOGGER.info("加载资源:{},{},{}", location, resource.getURL(), resource.exists());
        return resource.getInputStream();
    }

    public static Resource[] getResources(String... locations) throws IOException {
        List<Resource> list = new ArrayList<>();
        for (String path : locations) {
            list.addAll(Arrays.asList(new PathMatchingResourcePatternResolver().getResources(path)));
        }
        return list.toArray(new Resource[list.size()]);
    }
}
