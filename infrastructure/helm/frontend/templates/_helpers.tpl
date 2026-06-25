{{- define "frontend.fullname" -}}
{{- printf "%s-frontend" .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "frontend.labels" -}}
{{ include "hms.labels" . }}
app.kubernetes.io/component: frontend
{{- end -}}

{{- define "frontend.selectorLabels" -}}
app.kubernetes.io/name: hms-frontend
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/component: frontend
{{- end -}}
