package com.gk.study.config;

import com.gk.study.interceptor.AccessInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class MyConfig implements WebMvcConfigurer {
    private static final Logger logger = LoggerFactory.getLogger(MyConfig.class);

    @Value("${File.uploadPath}")
    private String uploadPath;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")//项目中的所有接口都支持跨域
                .allowedOriginPatterns("*") //所有地址都可以访问，也可以配置具体地址
                .allowCredentials(true)
                .allowedMethods("*");//"GET", "HEAD", "POST", "PUT", "DELETE", "OPTIONS"
    }


    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 添加swagger静态资源
        registry.addResourceHandler("doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**").addResourceLocations("classpath:/META-INF/resources/webjars/");
        
        // 添加文件上传目录的访问配置
        String location = "file:" + uploadPath.replace("\\", "/") + "/";
        logger.info("Static resource location: " + location);
        
        registry.addResourceHandler("/upload/**")
                .addResourceLocations(location)
                .setCachePeriod(3600)
                .resourceChain(false);

        // 添加头像目录的访问配置
        registry.addResourceHandler("/api/upload/**")
                .addResourceLocations(location)
                .setCachePeriod(3600)
                .resourceChain(false);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 自定义拦截器
        registry.addInterceptor(new AccessInterceptor());
    }
}
