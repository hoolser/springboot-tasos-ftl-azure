package com.tasos.demo.service;

import org.springframework.stereotype.Service;

@Service
public class MessageService {

    public String getMessage() {
        return "Hello from the Home service!";
    }

    public String getMessageForTasos() {
        return "Hello from the Tasos Rodopoli service!";
    }

    public String getMessageForBlob() {
        return "Hello from the Blob Storage service!";
    }

    public String getMessageForMarina() {
        return "Se agapw polu Mwro mou!";
    }

}
