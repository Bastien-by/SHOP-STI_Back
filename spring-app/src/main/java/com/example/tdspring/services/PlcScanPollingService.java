package com.example.tdspring.services;

import com.example.tdspring.dto.ScanEventDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Point unique de polling du PLC pour la douchette.
 *
 * Avant : chaque page Angular (login, scan, check, dépôt) ouvrait son
 * propre setInterval et appelait directement le backend, donc le web
 * server du S7-1200 recevait autant de flux de polling que de postes/pages
 * ouvertes simultanément.
 *
 * Maintenant : un seul job lit "Data".Scan à intervalle fixe, quel que soit
 * le nombre de clients connectés, et diffuse la valeur via WebSocket
 * (/topic/scan). Le nombre de requêtes vers l'automate ne dépend donc plus
 * du nombre de postes utilisateurs.
 */
@Service
@Slf4j
public class PlcScanPollingService {

    private static final long POLL_INTERVAL_MS = 800;

    @Autowired
    private SiemensPlcService siemensPlcService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Scheduled(fixedDelay = POLL_INTERVAL_MS)
    public void pollScan() {
        String value = siemensPlcService.readScan();

        if (value == null || value.isEmpty()) {
            return;
        }

        log.info("Scan détecté par le polling centralisé : '{}'", value);

        // On efface immédiatement côté PLC pour ne pas rediffuser la même
        // valeur au cycle suivant, puis on pousse l'événement aux clients.
        siemensPlcService.clearScan();

        messagingTemplate.convertAndSend(
                "/topic/scan",
                new ScanEventDTO(true, value, System.currentTimeMillis())
        );
    }
}
