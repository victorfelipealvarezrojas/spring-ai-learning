#!/bin/bash

# https://huggingface.co/models?inference_provider=hf-inference&sort=trending
# Buscar modelos provistos por la shared API - usar inference_provider=hf-inference

PROMPT="summarize: En la reunión de planificación del tercer trimestre celebrada hoy, el equipo debatió sobre cómo mejorar tanto la fiabilidad técnica como la experiencia del usuario de nuestra solución de pago móvil."

# brew install jq
# jq construye el JSON payload de forma segura. Es una herramienta de línea de comandos para procesar y manipular JSON.

# Payload final
JSON_PAYLOAD=$(jq -n --arg inputs "$PROMPT" '{
  inputs: $inputs,
  parameters: { max_new_tokens: 20, temperature: 0.7 },
  options: { wait_for_model: true }
}')

echo "Request payload:"
echo "$JSON_PAYLOAD" | jq .

# https://huggingface.co/facebook/bart-large-cnn?inference_provider=hf-inference
MODEL="facebook/bart-large-cnn"

# Llamada a la shared Inference API
curl https://router.huggingface.co/hf-inference/models/$MODEL \
  -H "Authorization: Bearer $HUGGINGFACE_API_KEY" \
  -H "Content-Type: application/json" \
  -d "$JSON_PAYLOAD"