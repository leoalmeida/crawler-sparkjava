# crawler-sparkjava

[Português](README.md) | [English](README.en.md)

![Java](https://img.shields.io/badge/java-17-orange)
![SparkJava](https://img.shields.io/badge/sparkjava-2.9.4-red)
![Status](https://img.shields.io/badge/status-active-brightgreen)

Crawler web assíncrono construído com Java + SparkJava, com API REST para criar, acompanhar e listar jobs de rastreamento.

## Sumario

- [Visao Geral](#visao-geral)
- [Requisitos](#requisitos)
- [Configuracao](#configuracao)
- [Como Rodar](#como-rodar)
- [API](#api)
- [Testes e Qualidade](#testes-e-qualidade)
- [Docker](#docker)
- [Estrutura](#estrutura)
- [Troubleshooting](#troubleshooting)

## Visao Geral

- API REST com endpoints em `/crawl`
- processamento em background com `ExecutorService`
- armazenamento em memoria thread-safe (`ConcurrentHashMap`)
- desligamento gracioso via shutdown hook

## Requisitos

- JDK 17+
- Maven 3.6+
- Docker (opcional)

## Configuracao

Variaveis esperadas em runtime:

- `BASE_URL`: URL base alvo para o crawler (obrigatoria)
- `PORT`: porta HTTP da API (opcional, default `8081`)

Exemplo no PowerShell:

```powershell
$env:BASE_URL="https://example.com"
$env:PORT="8081"
```

## Como Rodar

```powershell
Set-Location "c:\Users\leo_a\projetos\crawler-sparkjava"
mvn clean install
mvn exec:java
```

API disponivel em `http://localhost:8081`.

## API

### `POST /crawl`

Cria um novo job assíncrono.

Request:

```json
{
  "keyword": "sparkjava"
}
```

Response `201`:

```json
{
  "id": "a1b2c3d4"
}
```

### `GET /crawl/:id`

Consulta status e URLs encontradas para um job.

### `GET /crawl`

Lista todos os jobs criados.

## Testes e Qualidade

Rodar todos os testes:

```powershell
mvn test
```

Rodar pipeline de validacao completa (testes + analise):

```powershell
mvn verify
```

Formatar codigo:

```powershell
mvn spotless:apply
```

## Docker

Build da imagem:

```powershell
docker build -t crawler-sparkjava .
```

Execucao do container:

```powershell
docker run --rm -p 8081:8081 -e BASE_URL="https://example.com" crawler-sparkjava
```

## Estrutura

```text
crawler-sparkjava/
  src/main/java/space/lasf/sparkjava/
    controller/
    dao/
    dto/
    entity/
    exception/
    handler/
    helper/
    route/
    Main.java
  src/test/java/space/lasf/sparkjava/
  pom.xml
  Dockerfile
```

## Troubleshooting

- Erro `BASE_URL environment variable not set`: defina `BASE_URL` antes de iniciar.
- Porta em uso: ajuste `PORT` no ambiente.
- Falha em testes de integracao: execute `mvn clean test` para limpar estado previo.