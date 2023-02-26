package com.project.ecomapp.ecommerce_Project.Config;

import com.project.ecomapp.ecommerce_Project.Services.CustomUserDetailsService;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private JWTUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        final String requestHeaderToken = request.getHeader("Authorization");
        System.out.println(requestHeaderToken);
        String username = null;
        String jwtToken = null;
        if(requestHeaderToken!=null && requestHeaderToken.startsWith("Bearer ")){

            jwtToken = requestHeaderToken.substring(7);
            try {
                username = this.jwtUtils.extractUsername(jwtToken);
            }catch (ExpiredJwtException e){
                e.printStackTrace();
                System.out.println("token expired");
            }catch(Exception e){
                e.printStackTrace();
                System.out.println("error found");
            }

        } else{
            System.out.println("Invalid token or null token");
        }

        //validated
        if(username!=null && SecurityContextHolder.getContext().getAuthentication()==null){

            final UserDetails userDetails = this.customUserDetailsService.loadUserByUsername(username);
            if(jwtUtils.validateToken(jwtToken,userDetails)){
                //token is valid

                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(userDetails,null,userDetails.getAuthorities());
                usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);

            }else{
                System.out.println("token is not valid");
            }


        }
        filterChain.doFilter(request,response);

    }

}
