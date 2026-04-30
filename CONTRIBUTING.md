# Contributing

This repository is being built as a portfolio-grade modular monolith. Keep changes small, tested, and easy to review.

## Workflow

1. Check the current state with `git status --short --branch`.
2. Create a focused branch for implementation work.
3. Make one logical change at a time.
4. Run the relevant backend and frontend checks.
5. Commit with a clear message.
6. Push the branch so work is backed up.

## Local Checks

Backend:

```bash
cd server
./mvnw test
```

Frontend:

```bash
cd ops-console
npm install
npm run lint
npm run build
```
