package com.supportportal.supportportal.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supportportal.supportportal.config.constant.SecurityConstant;
import com.supportportal.supportportal.domain.HttpResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Component
public class JWTAuthenticationEntryPoint extends Http403ForbiddenEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) throws IOException {
    HttpResponse httpResponse = new HttpResponse(
            HttpStatus.FORBIDDEN.value(),
            HttpStatus.FORBIDDEN,
            HttpStatus.FORBIDDEN.getReasonPhrase().toUpperCase(Locale.ROOT),
            SecurityConstant.FORBIDDEN_MESSAGE);
    response.setContentType(APPLICATION_JSON_VALUE);
    response.setStatus(HttpStatus.FORBIDDEN.value());
        OutputStream outputStream = response.getOutputStream();
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(outputStream, httpResponse);
        outputStream.flush();
    }
}
