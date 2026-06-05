package prm393.group8.flowermanagement.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to log incoming HTTP requests and their processing time.
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        long startTime = System.currentTimeMillis();
        
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();
        String clientIp = request.getRemoteAddr();

        // Build the complete URI with query parameters if present
        String fullUri = uri;
        if (queryString != null) {
            fullUri += "?" + queryString;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            int status = response.getStatus();

            // Log format: HTTP GET /api/products?id=5 | Status: 200 | IP: 127.0.0.1 | Duration: 15ms
            logger.info("HTTP {} {} | Status: {} | IP: {} | Duration: {}ms", 
                    method, fullUri, status, clientIp, duration);
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.startsWith("/css/") || 
               path.startsWith("/js/") || 
               path.startsWith("/image/") || 
               path.equals("/favicon.ico");
    }
}

