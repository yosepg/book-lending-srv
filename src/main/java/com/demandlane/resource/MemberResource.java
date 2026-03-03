package com.demandlane.resource;

import com.demandlane.config.Roles;
import com.demandlane.dto.MemberRequest;
import com.demandlane.dto.MemberResponse;
import com.demandlane.dto.SuccessResponse;
import com.demandlane.entity.Member;
import com.demandlane.service.MemberService;
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

@Path("/api/v1/members")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Members", description = "Library member management")
public class MemberResource {

    @Inject
    MemberService memberService;

    @GET
    @RolesAllowed(Roles.ADMIN)
    @Operation(summary = "List all members")
    public SuccessResponse<List<MemberResponse>> list() {
        List<MemberResponse> members = memberService.listAll().stream().map(MemberResponse::from).toList();
        return SuccessResponse.of(members);
    }

    @GET
    @Path("/{id}")
    @RolesAllowed(Roles.ADMIN)
    @Operation(summary = "Get a member by ID")
    public SuccessResponse<MemberResponse> get(@PathParam("id") Long id) {
        return SuccessResponse.of(MemberResponse.from(memberService.findById(id)));
    }

    @POST
    @RolesAllowed(Roles.ADMIN)
    @Operation(summary = "Create a new member")
    public Response create(@Valid MemberRequest request) {
        Member created = memberService.create(request);
        return Response.created(URI.create("/api/members/" + created.id))
                .entity(SuccessResponse.of(MemberResponse.from(created)))
                .build();
    }

    @PUT
    @Path("/{id}")
    @RolesAllowed(Roles.ADMIN)
    @Operation(summary = "Update an existing member")
    public SuccessResponse<MemberResponse> update(@PathParam("id") Long id, @Valid MemberRequest request) {
        return SuccessResponse.of(MemberResponse.from(memberService.update(id, request)));
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed(Roles.ADMIN)
    @Operation(summary = "Delete a member")
    public Response delete(@PathParam("id") Long id) {
        memberService.delete(id);
        return Response.noContent().build();
    }
}
