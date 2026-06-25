{{/*
Common helper templates for HMS Helm charts.
*/}}

{{/* Generate a fullname: <release>-<chart> */}}
{{- define "hms.fullname" -}}
{{- printf "%s-%s" .Release.Name .Chart.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/* Common labels */}}
{{- define "hms.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
app.kubernetes.io/part-of: hms
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{/* Selector labels */}}
{{- define "hms.selectorLabels" -}}
app.kubernetes.io/name: {{ .Chart.Name }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{/* Namespace */}}
{{- define "hms.namespace" -}}
{{- default "hms-production" .Values.namespace -}}
{{- end -}}

{{/* Image full reference */}}
{{- define "hms.image" -}}
{{- printf "%s:%s" .repository (.tag | default "latest") -}}
{{- end -}}
