# MegaFlix

MegaFlix e o Nuvio com a cara do MegaFlix: tema laranja sobre preto, logo e nome proprios,
TMDB em portugues do Brasil ligado por padrao e o YouCine como opcao.

Como funciona este fork:

- O branch `dev` acompanha o Nuvio oficial.
- O branch `megaflix` = `dev` + esta pasta `megaflix/` + `.github/workflows/megaflix-build.yml`.
- A cada push no `megaflix`, o GitHub Actions roda `python3 megaflix/apply.py` (aplica as mudancas
  do MegaFlix por cima do codigo do Nuvio), compila e publica o `MegaFlix.apk` em **Releases**.
- O relatorio da ultima compilacao fica no branch `megaflix-logs` (`last-build.md`).

Segredo necessario (Settings > Secrets and variables > Actions):

- `TMDB_API_KEY`: chave da API do TMDB (titulos e sinopses em portugues).
- `MEGAFLIX_KEYSTORE`: chave de assinatura do app em base64 (para cada versao nova instalar por cima da anterior).

Licenca: GPL-3.0, como o Nuvio. O codigo do MegaFlix fica aberto neste repositorio.
