{{- define "keycloak.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "keycloak.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name (include "keycloak.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "keycloak.labels" -}}
app.kubernetes.io/name: {{ include "keycloak.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" }}
{{- end -}}

{{- define "keycloak.selectorLabels" -}}
app.kubernetes.io/name: {{ include "keycloak.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "keycloak.dbSecretName" -}}
{{- if .Values.db.existingSecret -}}
{{ .Values.db.existingSecret }}
{{- else -}}
{{ include "keycloak.fullname" . }}-db
{{- end -}}
{{- end -}}

{{- define "keycloak.adminSecretName" -}}
{{- if .Values.admin.existingSecret -}}
{{ .Values.admin.existingSecret }}
{{- else -}}
{{ include "keycloak.fullname" . }}-admin
{{- end -}}
{{- end -}}
