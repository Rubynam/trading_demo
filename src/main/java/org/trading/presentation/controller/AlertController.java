package org.trading.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.trading.application.command.AlertCommand;
import org.trading.presentation.request.AlertRequest;
import org.trading.presentation.response.AlertResponse;

@RestController
@RequestMapping("/alert")
@RequiredArgsConstructor
public class AlertController {

    private final AlertCommand alertCommand;

    @PostMapping
    public ResponseEntity<AlertResponse> submitAlert(@RequestBody AlertRequest request) throws Exception {
        return ResponseEntity.ok().body(alertCommand.execute(request));
    }
}
