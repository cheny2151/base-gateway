package com.chenyi.base.gateway.constants;

/**
 * 网关常量
 *
 * @author by chenyi
 * @date 2022/5/26
 */
public class GatewayConstants {

    public static class Auth {
        public final static String USER_TOKEN_KEY = "token";
        public final static String USER_INFO_KEY = "USER";
        public final static String USER_ID_KEY = "userId";
        public final static String USER_NAME_KEY = "userName";
        public final static String USER_ROLES_KEY = "userRoles";
        public final static String USER_TENANT_KEY = "userTenant";
    }

    public static class Content {
        public final static String REQUEST_BODY_OBJECT_KEY = "cachedRequestBodyObject";
        public final static String USER_KEY = "UserObject";
    }


}
