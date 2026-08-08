package com.demetrius.tribunal.common.auth;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口级权限校验注解（RBAC）。
 *
 * <p>标注在 Controller 方法上，声明调用该接口所需的权限码；由 {@link AuthInterceptor}
 * 解析并校验当前用户（UserContext）是否拥有任一权限，无权限返回 403。</p>
 *
 * <p>示例：</p>
 * <pre>
 * {@code @RequirePermission("order:review")}
 * {@code @PostMapping("/{id}/review")}
 * public ApiResponse&lt;OrderResult&gt; review(...) { ... }
 * </pre>
 *
 * <p>未标注此注解的接口仅要求登录（有有效 Token），不要求特定权限。</p>
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * 所需权限码（如 "order:review"）；多个为"任一"满足即可（OR 语义）。
     */
    String[] value();
}