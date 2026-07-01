package com.datacopilotx.ai.config;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class ElasticSearchConfig {

    @Value("${elasticsearch.host}")
    String host;
    @Value("${elasticsearch.port}")
    Integer port;
    @Value("${elasticsearch.scheme}")
    String scheme;
    @Value("${elasticsearch.username:}")
    String username;
    @Value("${elasticsearch.password:}")
    String password;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        RestClient restClient = RestClient.builder(new HttpHost(host, port, scheme))
                .setHttpClientConfigCallback(httpClientBuilder -> {
                    if (username != null && !username.isEmpty()) {
                        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                        credentialsProvider.setCredentials(AuthScope.ANY,
                                new UsernamePasswordCredentials(username, password));
                        httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
                    return httpClientBuilder;
                })
                .build();

        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}