package com.demandlane.resource;

import com.demandlane.config.Roles;
import com.demandlane.dto.BorrowRequest;
import com.demandlane.dto.LoanResponse;
import com.demandlane.dto.SuccessResponse;
import com.demandlane.entity.Loan;
import com.demandlane.service.LoanService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/loans")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Loans", description = "Book borrowing and returning")
public class LoanResource {

    @Inject
    LoanService loanService;

    @GET
    @RolesAllowed(Roles.ADMIN)
    @Operation(summary = "List loans", description = "Filter by memberId or active status")
    public SuccessResponse<List<LoanResponse>> list(@QueryParam("memberId") Long memberId,
                                                    @QueryParam("active") Boolean active) {
        List<Loan> loans;
        if (memberId != null) {
            loans = loanService.listByMemberId(memberId);
        } else if (Boolean.TRUE.equals(active)) {
            loans = loanService.listActive();
        } else {
            loans = loanService.listAll();
        }
        return SuccessResponse.of(loans.stream().map(LoanResponse::from).toList());
    }

    @GET
    @Path("/{id}")
    @RolesAllowed({Roles.ADMIN, Roles.USER})
    @Operation(summary = "Get a loan by ID")
    public SuccessResponse<LoanResponse> get(@PathParam("id") Long id) {
        return SuccessResponse.of(LoanResponse.from(loanService.findById(id)));
    }

    @POST
    @Path("/borrow")
    @RolesAllowed({Roles.ADMIN, Roles.USER})
    @Operation(summary = "Borrow a book")
    public Response borrow(@Valid BorrowRequest request) {
        Loan loan = loanService.borrowBook(request.bookId(), request.memberId());
        return Response.created(URI.create("/api/loans/" + loan.id))
                .entity(SuccessResponse.of(LoanResponse.from(loan)))
                .build();
    }

    @POST
    @Path("/{id}/return")
    @Consumes(MediaType.WILDCARD)
    @RolesAllowed({Roles.ADMIN, Roles.USER})
    @Operation(summary = "Return a borrowed book")
    public SuccessResponse<LoanResponse> returnBook(@PathParam("id") Long id) {
        return SuccessResponse.of(LoanResponse.from(loanService.returnBook(id)));
    }
}
