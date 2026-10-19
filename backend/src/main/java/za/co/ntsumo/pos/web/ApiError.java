package za.co.ntsumo.pos.web;

import java.util.List;

/** What the till shows the cashier when something is refused. */
public record ApiError(String message, List<String> reasons) {

    public static ApiError of(String message) {
        return new ApiError(message, List.of());
    }
}
