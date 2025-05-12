package otp.controllers;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpContext;
import otp.model.UserRole;

public class Dispatcher {
    private final AuthController authController = new AuthController();
    private final OtpController otpController = new OtpController();
    private final AdminController adminController = new AdminController();

    public void registerRoutes(HttpServer server) {
        server.createContext("/register", authController::handleRegister);
        server.createContext("/login",    authController::handleLogin);

        HttpContext genCtx = server.createContext("/pass/generate", otpController::generateOtp);
        genCtx.getFilters().add(new AuthFilter(UserRole.USER));
        HttpContext valCtx = server.createContext("/pass/validate", otpController::validateOtp);
        valCtx.getFilters().add(new AuthFilter(UserRole.USER));

        HttpContext configCtx = server.createContext("/admin/config", adminController::updateOtpConfig);
        configCtx.getFilters().add(new AuthFilter(UserRole.ADMIN));
        HttpContext usersCtx = server.createContext("/admin/users", exchange -> {
            String method = exchange.getRequestMethod();
            if ("GET".equalsIgnoreCase(method)) {
                adminController.listUsers(exchange);
            } else if ("DELETE".equalsIgnoreCase(method)) {
                adminController.deleteUser(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        });
        usersCtx.getFilters().add(new AuthFilter(UserRole.ADMIN));
    }
}
