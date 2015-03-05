package com.deroxas.template.resources;

import com.deroxas.template.core.Person;
import com.deroxas.template.db.PersonDAO;
import com.deroxas.template.views.PersonView;
import com.google.common.base.Optional;
import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.jersey.params.LongParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

@Path("/people/{personId}")
@Produces(MediaType.APPLICATION_JSON)
public class PersonResource {

    private final PersonDAO peopleDAO;

    public PersonResource(PersonDAO peopleDAO) {
        this.peopleDAO = peopleDAO;
    }

    @GET
    @UnitOfWork
    public Person getPerson(@PathParam("personId") LongParam personId) {
        return findSafely(personId.get());
    }

    @GET
    @Path("/view_freemarker")
    @UnitOfWork
    @Produces(MediaType.TEXT_HTML)
    public PersonView getPersonViewFreemarker(@PathParam("personId") LongParam personId) {
        return new PersonView(PersonView.Template.FREEMARKER, findSafely(personId.get()));
    }

    @GET
    @Path("/view_mustache")
    @UnitOfWork
    @Produces(MediaType.TEXT_HTML)
    public PersonView getPersonViewMustache(@PathParam("personId") LongParam personId) {
        return new PersonView(PersonView.Template.MUSTACHE, findSafely(personId.get()));
    }

    private Person findSafely(long personId) {
        final Optional<Person> person = peopleDAO.findById(personId);
        if (!person.isPresent()) {
            throw new NotFoundException("No such user.");
        }
        return person.get();
    }
}
