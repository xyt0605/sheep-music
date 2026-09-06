package com.example.sheepmusic.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 接口文档配置（springdoc / OpenAPI 3，knife4j 4.x 渲染 /doc.html）
 *
 * 旧版 springfox Docket 已随 Boot 3 移除，全局 Token 请求头改用
 * OperationCustomizer 实现：每个接口都带一个可选的 Authorization 头参数。
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI sheepMusicOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sheep Music API文档")
                        .description("在线音乐网站后端接口文档")
                        .version("1.0.0")
                        .contact(new Contact().name("Sheep Music Team")));
    }

    @Bean
    public OperationCustomizer globalTokenHeader() {
        return (operation, handlerMethod) -> {
            operation.addParametersItem(new Parameter()
                    .in("header")
                    .name("Authorization")
                    .description("JWT Token（格式：Bearer xxx）")
                    .required(false)
                    .schema(new io.swagger.v3.oas.models.media.StringSchema()));
            return operation;
        };
    }
}
