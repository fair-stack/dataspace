package cn.cnic.dataspace.api.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.RequestHandler;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;

@EnableSwagger2
@ConditionalOnProperty(prefix = "swagger", value = { "enable" }, havingValue = "true")
@Configuration
public class SwaggerConfig {

    @Bean
    public Docket createDocket() {
        return new Docket(DocumentationType.SWAGGER_2).apiInfo(new ApiInfoBuilder().title("data-space api swagger").description("data-space  API Document").version("1.0").build()).select().apis(basePackage("cn.cnic.dataspace.api.controller;cn.cnic.dataspace.api.datax.admin.controller")).paths(PathSelectors.any()).build();
    }

    public static Predicate<RequestHandler> basePackage(final String basePackage) {
        return input -> declaringClass(input).transform(handlerPackage(basePackage)).or(true);
    }

    private static Function<Class<?>, Boolean> handlerPackage(final String basePackage) {
        return input -> {
            // Loop judgment matching
            for (String strPackage : basePackage.split(";")) {
                boolean isMatch = input.getPackage().getName().startsWith(strPackage);
                if (isMatch) {
                    return true;
                }
            }
            return false;
        };
    }

    private static Optional<? extends Class<?>> declaringClass(RequestHandler input) {
        return Optional.fromNullable(input.declaringClass());
    }
}
