{{- define "backend.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "backend.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name (include "backend.name" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "backend.labels" -}}
app.kubernetes.io/name: {{ include "backend.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" }}
{{- end -}}

{{- define "backend.selectorLabels" -}}
app.kubernetes.io/name: {{ include "backend.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "backend.dbSecretName" -}}
{{- if .Values.db.existingSecret -}}
{{ .Values.db.existingSecret }}
{{- else -}}
{{ include "backend.fullname" . }}-db
{{- end -}}
{{- end -}}

{{- define "backend.keycloakSecretName" -}}
{{- if .Values.keycloak.admin.existingSecret -}}
{{ .Values.keycloak.admin.existingSecret }}
{{- else -}}
{{ include "backend.fullname" . }}-keycloak
{{- end -}}
{{- end -}}

{{- define "backend.objectStorageSecretName" -}}
{{- if .Values.objectStorage.existingSecret -}}
{{ .Values.objectStorage.existingSecret }}
{{- else -}}
{{ include "backend.fullname" . }}-objectstorage
{{- end -}}
{{- end -}}
