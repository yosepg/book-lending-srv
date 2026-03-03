package com.demandlane.resource;

import com.demandlane.config.Roles;
import com.demandlane.dto.BookRequest;
import com.demandlane.dto.BookResponse;
import com.demandlane.dto.SuccessResponse;
import com.demandlane.entity.Book;
import com.demandlane.service.BookService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/books")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Books", description = "Book catalog management")
public class BookResource {

    @Inject
    BookService bookService;

    @GET
    @RolesAllowed({Roles.ADMIN, Roles.USER})
    @Operation(summary = "List all books")
    public SuccessResponse<List<BookResponse>> list() {
        List<BookResponse> books = bookService.listAll().stream().map(BookResponse::from).toList();
        return SuccessResponse.of(books);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({Roles.ADMIN, Roles.USER})
    @Operation(summary = "Get a book by ID")
    public SuccessResponse<BookResponse> get(@PathParam("id") Long id) {
        return SuccessResponse.of(BookResponse.from(bookService.findById(id)));
    }

    @POST
    @RolesAllowed(Roles.ADMIN)
    @Operation(summary = "Create a new book")
    public Response create(@Valid BookRequest request) {
        Book created = bookService.create(request);
        return Response.created(URI.create("/api/books/" + created.id))
                .entity(SuccessResponse.of(BookResponse.from(created)))
                .build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed(Roles.ADMIN)
    @Operation(summary = "Update an existing book")
    public SuccessResponse<BookResponse> update(@PathParam("id") Long id, @Valid BookRequest request) {
        return SuccessResponse.of(BookResponse.from(bookService.update(id, request)));
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed(Roles.ADMIN)
    @Operation(summary = "Delete a book")
    public Response delete(@PathParam("id") Long id) {
        bookService.delete(id);
        return Response.noContent().build();
    }
}
