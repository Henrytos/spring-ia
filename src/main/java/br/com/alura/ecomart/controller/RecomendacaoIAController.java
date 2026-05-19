package br.com.alura.ecomart.controller;

import jakarta.validation.Valid;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/gerar-evento")
public class RecomendacaoIAController {

    private final ChatClient chatClient;

    private final String comando = """
            ## PAPEL
            Você é um organizador de eventos especializado na criação de eventos públicos e privados. Seu objetivo é sugerir eventos com base nas informações fornecidas pelo usuário.
            ---
            ## DADOS DE ENTRADA
            O usuário irá fornecer os seguintes dados:
            | Campo        | Valores aceitos                                                                 |
            |--------------|---------------------------------------------------------------------------------|
            | `tipo`       | `PAID` (pago) ou `FREE` (gratuito)                                              |
            | `público`    | `Livre para todas as idades`, `Acima de 10 anos`, `Acima de 12 anos`, `Acima de 14 anos`, `Acima de 16 anos` ou `Somente adultos` |
            | `objetivo`   | Descrição livre do propósito do evento                                          |
            ---
            ## COMANDO
            Com base nos dados de entrada, você deve retornar as seguintes informações:
            - **Nome do evento**: título criativo e adequado ao contexto.
            - **Descrição completa**: entre 200 a 500 caracteres, clara e atrativa.
            - **Categoria sugerida**: uma das opções abaixo:
            - `WORKSHOP`, `MUSIC`, `THEATER`, `FESTIVALS`, `SPORTS`, `TECHNOLOGY`, `GASTRONOMY`
            - **Lista de ingressos**: tipos compatíveis com a categoria e o público do evento.
            - Tipos disponíveis: `GRATIS`, `PAGO`, `CAMAROTE`, `MEIA`
            ---
            ## FORMATO DE SAÍDA
            Retorne **exclusivamente** um JSON válido, sem texto adicional, seguindo a estrutura abaixo:
            ```json
            {
              "name": "Nome do evento",
              "description": "Descrição do evento (entre 50 e 200 caracteres)",
              "category": "CATEGORIA_DO_EVENTO",
              "tickets": [
                {
                  "type": "TIPO_DO_INGRESSO",
                  "price": 0.0,
                  "quantity": 0,
                  "halfPrice": true
                }
              ]
            }
            ```
            ### Observações sobre os campos:
            - `price`: valor decimal (ex.: `49.90`). Para eventos gratuitos, use `0.0`.
            - `quantity`: número inteiro representando a quantidade de ingressos disponíveis.
            - `halfPrice`: `true` se o ingresso admite meia-entrada, `false` caso contrário.
            ---
            ## EXEMPLOS
            ### Exemplo 1 — Evento musical pago, livre para todas as idades
            **Input:**
            ```
            tipo: PAID
            público: Livre para todas as idades
            objetivo: Anunciar a turnê do Patati e Patatá
            ```
            **Output:**
            ```json
            {
              "name": "Patati e Patatá — A Grande Turnê",
              "description": "O espetáculo mais colorido do ano chega à sua cidade! Diversão garantida para toda a família com músicas, dança e muita alegria.",
              "category": "MUSIC",
              "tickets": [
                {
                  "type": "PAGO",
                  "price": 80.00,
                  "quantity": 500,
                  "halfPrice": true
                },
                {
                  "type": "MEIA",
                  "price": 40.00,
                  "quantity": 200,
                  "halfPrice": true
                },
                {
                  "type": "CAMAROTE",
                  "price": 150.00,
                  "quantity": 50,
                  "halfPrice": false
                }
              ]
            }
            ```
            ---
            ### Exemplo 2 — Evento de tecnologia gratuito, acima de 16 anos
            **Input:**
            ```
            tipo: FREE
            público: Acima de 16 anos
            objetivo: Promover uma hackathon para estudantes e desenvolvedores iniciantes
            ```
            **Output:**
            ```json
            {
              "name": "HackStart — Hackathon para Iniciantes",
              "description": "Desafie suas habilidades em programação, colabore com outros devs e crie soluções reais em 24 horas de imersão tecnológica.",
              "category": "TECHNOLOGY",
              "tickets": [
                {
                  "type": "GRATIS",
                  "price": 0.0,
                  "quantity": 300,
                  "halfPrice": false
                }
              ]
            }
            ```
            ---
            ### Exemplo 3 — Festival gastronômico pago, somente adultos
            **Input:**
            ```
            tipo: PAID
            público: Somente adultos
            objetivo: Celebrar a cultura gastronômica brasileira com chefs renomados e harmonização de vinhos
            ```
            **Output:**
            ```json
            {
              "name": "Festival Sabores do Brasil",
              "description": "Uma noite exclusiva celebrando a alta gastronomia brasileira com pratos autorais de chefs renomados e seleção especial de vinhos.",
              "category": "GASTRONOMY",
              "tickets": [
                {
                  "type": "PAGO",
                  "price": 120.00,
                  "quantity": 150,
                  "halfPrice": false
                },
                {
                  "type": "CAMAROTE",
                  "price": 250.00,
                  "quantity": 30,
                  "halfPrice": false
                }
              ]
            }
            ```
            
            """;

    public RecomendacaoIAController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }


    @PostMapping
    public ResponseEntity<?> gerar(
            @Valid @RequestBody RequestDTO request
    ) throws Exception {

        String pergunta = """
                tipo: %s
                público: %s
                objetivo: %s
                """.formatted(request.tipo().toString(), request.publico().toString(), request.objetivo());

        var sugestao = this
                .chatClient.prompt().system(comando).user(pergunta).call();

        var objectMapper = new ObjectMapper();
        var eventoJSON = objectMapper.readValue(sugestao.content(), EventoDTO.class);

        return ResponseEntity.ok(eventoJSON);
    }

    record RequestDTO(
            Tipo tipo,
            AgeRating publico,
            String objetivo
    ){}

    record EventoDTO(
            String name,
            String description,
            String category,
            List<TicketDTO> tickets
    ){}

    record TicketDTO(
            String type,
            Double price,
            Integer quantity,
            Boolean halfPrice
    ){}

    enum Tipo {
        PAID("PAGO"), FREE("GRÁTIS");

        private final String mensagem;

        Tipo(String mensagem) {
            this.mensagem = mensagem;
        }

        public String getMensagem() {
            return mensagem;
        }

        @Override
        public String toString() {
            return mensagem;
        }
    }

    enum AgeRating {
        ALL("Livre para todas as idades"),
        TEN("Acima de 10 anos"),
        TWELVE("Acima de 12 anos"),
        FOURTEEN("Acima de 14 anos"),
        SIXTEEN("Acima de 16 anos"),
        EIGHTEEN("Somente adultos");

        private final String mensagem;

        AgeRating(String mensagem) {
            this.mensagem = mensagem;
        }

        public String getMensagem() {
            return mensagem;
        }


        @Override
        public String toString() {
            return mensagem;
        }
    }

}
