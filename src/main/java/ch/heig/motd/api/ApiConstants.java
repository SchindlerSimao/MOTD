package ch.heig.motd.api;

public final class ApiConstants {
    private ApiConstants() {}

    public static final class Paths {
        private Paths() {}
        public static final String AUTH_REGISTER = "/auth/register";
        public static final String AUTH_LOGIN = "/auth/login";
        public static final String AUTH_LOGOUT = "/auth/logout";
        public static final String AUTH_DELETE = "/auth/delete";
        public static final String POSTS = "/posts";
        public static final String POST_WITH_ID = "/posts/{id}";
    }

    public static final class Keys {
        private Keys() {}
        public static final String USERNAME = "username";
        public static final String PASSWORD = "password";
        public static final String CONTENT = "content";
        public static final String TOKEN = "token";
        public static final String ERROR = "error";
    }

    public static final class Headers {
        private Headers() {}
        public static final String AUTHORIZATION = "Authorization";
        public static final String BEARER_PREFIX = "Bearer ";
    }

    public static final class Errors {
        private Errors() {}
        public static final String MISSING_USERNAME_OR_PASSWORD = "missing.username.or.password";
        public static final String USERNAME_EXISTS = "username.exists";
        public static final String INVALID_CREDENTIALS = "invalid.credentials";
        public static final String MISSING_TOKEN = "missing.token";
        public static final String INVALID_TOKEN = "invalid.token";
        public static final String UNAUTHORIZED = "unauthorized";
        public static final String EMPTY_CONTENT = "empty.content";
        public static final String NOT_FOUND = "not.found";
        public static final String FORBIDDEN = "forbidden";
        public static final String INTERNAL_ERROR = "internal.error";
    }
}
