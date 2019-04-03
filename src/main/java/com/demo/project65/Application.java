package com.demo.project65;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.Id;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.data.r2dbc.repository.query.Query;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@SpringBootApplication
@Slf4j
public class Application implements ApplicationRunner {

    @Autowired
    CustomerRepository repo;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("Seeding data!");
        Flux<String> names = Flux.just("raj", "david", "pam").delayElements(Duration.ofSeconds(1));
        Flux<Integer> colors = Flux.just(25, 27, 30).delayElements(Duration.ofSeconds(1));
        Flux<Customer> customers = Flux.zip(names, colors).map(tupple -> {
            return new Customer(null, tupple.getT1(), tupple.getT2());
        });
        repo.deleteAll().thenMany(customers.flatMap(c -> repo.save(c))
                .thenMany(repo.findAll())).subscribe(System.out::println);
    }
}

@RestController
@RequestMapping("/api")
class AppController {

    @Autowired
    CustomerRepository repo;

    @GetMapping("/all")
    public Flux<Customer> findAll() {
        return repo.findAll();
    }

    @GetMapping("/id/{customerId}")
    public Mono<Customer> findById(@PathVariable Long customerId) {
        return repo.findById(customerId);
    }

    @PostMapping(value = "/save", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public Mono<Customer> save(@RequestBody Customer customer) {
        return repo.save(customer);
    }

    @GetMapping("/find")
    public Flux<Customer> findById(@RequestParam String name, @RequestParam Integer age) {
        return repo.findByNameAndAge(name, age);
    }
}

@Configuration
@EnableR2dbcRepositories
class DatabaseConfiguration extends AbstractR2dbcConfiguration {
    @Bean
    public ConnectionFactory connectionFactory() {
        return new PostgresqlConnectionFactory(
                PostgresqlConnectionConfiguration.builder()
                        .host("localhost")
                        .database("demodb")
                        .username("demouser")
                        .password("demopwd")
                        .build()
        );
    }

}

interface CustomerRepository extends ReactiveCrudRepository<Customer, Long> {

    @Query("select * from customer where name = $1 and age = $2")
    Flux<Customer> findByNameAndAge(String name, Integer age);
}

@Data
@AllArgsConstructor
@NoArgsConstructor
@Table("customer")
class Customer {
    @Id
    public Long id;
    public String name;
    public Integer age;
}