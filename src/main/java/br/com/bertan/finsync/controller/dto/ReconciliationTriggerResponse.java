package br.com.bertan.finsync.controller.dto;

import java.time.LocalDateTime;

public record ReconciliationTriggerResponse(LocalDateTime triggeredAt) {
}