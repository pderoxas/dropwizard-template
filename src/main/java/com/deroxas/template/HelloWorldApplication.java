package com.deroxas.template;

import com.deroxas.template.auth.ExampleAuthenticator;
import com.deroxas.template.cli.RenderCommand;
import com.deroxas.template.core.Person;
import com.deroxas.template.core.Template;
import com.deroxas.template.core.User;
import com.deroxas.template.db.PersonDAO;
import com.deroxas.template.filter.DateRequiredFeature;
import com.deroxas.template.health.TemplateHealthCheck;
import com.deroxas.template.resources.*;
import com.google.common.collect.ImmutableMap;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.auth.AuthFactory;
import io.dropwizard.auth.basic.BasicAuthFactory;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.hibernate.HibernateBundle;
import io.dropwizard.migrations.MigrationsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.views.ViewBundle;

public class HelloWorldApplication extends Application<HelloWorldConfiguration> {
    public static void main(String[] args) throws Exception {
        new HelloWorldApplication().run(args);
    }

    private final HibernateBundle<HelloWorldConfiguration> hibernateBundle =
            new HibernateBundle<HelloWorldConfiguration>(Person.class) {
                @Override
                public DataSourceFactory getDataSourceFactory(HelloWorldConfiguration configuration) {
                    return configuration.getDataSourceFactory();
                }
            };

    @Override
    public String getName() {
        return "hello-world";
    }

    @Override
    public void initialize(Bootstrap<HelloWorldConfiguration> bootstrap) {
        // Enable variable substitution with environment variables
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(
                        bootstrap.getConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        );

        bootstrap.addCommand(new RenderCommand());
        bootstrap.addBundle(new AssetsBundle());
        bootstrap.addBundle(new MigrationsBundle<HelloWorldConfiguration>() {
            @Override
            public DataSourceFactory getDataSourceFactory(HelloWorldConfiguration configuration) {
                return configuration.getDataSourceFactory();
            }
        });
        bootstrap.addBundle(hibernateBundle);
        bootstrap.addBundle(new ViewBundle<HelloWorldConfiguration>() {
            @Override
            public ImmutableMap<String, ImmutableMap<String, String>> getViewConfiguration(HelloWorldConfiguration configuration) {
                return configuration.getViewRendererConfiguration();
            }
        });
    }

    @Override
    public void run(HelloWorldConfiguration configuration, Environment environment) {
        final PersonDAO dao = new PersonDAO(hibernateBundle.getSessionFactory());
        final Template template = configuration.buildTemplate();

        environment.healthChecks().register("template", new TemplateHealthCheck(template));
        environment.jersey().register(DateRequiredFeature.class);

        environment.jersey().register(
                AuthFactory.binder(new BasicAuthFactory<>(new ExampleAuthenticator(), "SUPER SECRET STUFF", User.class)));
        environment.jersey().register(new HelloWorldResource(template));
        environment.jersey().register(new ViewResource());
        environment.jersey().register(new ProtectedResource());
        environment.jersey().register(new PeopleResource(dao));
        environment.jersey().register(new PersonResource(dao));
        environment.jersey().register(new FilteredResource());
    }
}
