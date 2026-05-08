package com.backend.api.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class SupabaseConfig {

    @Value("${supabase.url}")
    public String supabaseUrl;

    @Value("${supabase.key}")
    public String supabaseKey;

    @Value("${supabase.auth.key}")
    public String supabaseAuthKey;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(new JdkClientHttpRequestFactory());
    }
}
