package com.github.Silexj.payment_engine.controller;

import com.github.Silexj.payment_engine.dto.AccountDto;
import com.github.Silexj.payment_engine.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountDto.Response createAccount(@RequestBody @Valid AccountDto.CreateRequest request) {
        return accountService.createAccount(request);
    }

    @GetMapping("/{id}")
    public AccountDto.Response getAccount(@PathVariable Long id) {
        return accountService.getAccount(id);
    }

    @PostMapping("/{id}/top-up")
    public AccountDto.Response topUpBalance(@PathVariable Long id,
                                            @RequestBody @Valid AccountDto.TopUpRequest request) {
        if (!id.equals(request.accountId())) {
            throw new IllegalArgumentException("Path ID and Body ID must match");
        }
        return accountService.topUpBalance(request);
    }
}
