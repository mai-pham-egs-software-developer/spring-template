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

{{/*
Realm-export JSON for the bootstrap realm (see .Values.realm.import), in the format Keycloak's
own --import-realm startup import expects. Built from values here instead of a static file so the
backend client secret and redirect URIs stay driven by one place (values.yaml).
*/}}
{{- define "keycloak.realmJson" -}}
{{- $import := .Values.realm.import -}}
{{- $scheme := $import.scheme -}}
{{- $backend := $import.backendClient -}}
{{- $backendHost := $backend.host | default (printf "api.%s" $import.baseDomain) -}}
{{- $clients := list (dict
      "clientId" $backend.clientId
      "enabled" true
      "protocol" "openid-connect"
      "publicClient" false
      "standardFlowEnabled" true
      "directAccessGrantsEnabled" false
      "serviceAccountsEnabled" false
      "secret" $backend.secret
      "redirectUris" (list (printf "%s://%s%s" $scheme $backendHost $backend.redirectPath))
      "webOrigins" (list)
    ) -}}
{{- range $import.spaClients }}
{{- $host := .host | default (printf "%s.%s" .subdomain $import.baseDomain) -}}
{{- $origin := printf "%s://%s" $scheme $host -}}
{{- $clients = append $clients (dict
      "clientId" .clientId
      "enabled" true
      "protocol" "openid-connect"
      "publicClient" true
      "standardFlowEnabled" true
      "directAccessGrantsEnabled" false
      "redirectUris" (list (printf "%s/*" $origin))
      "webOrigins" (list $origin)
      "attributes" (dict "pkce.code.challenge.method" "S256")
    ) -}}
{{- end }}
{{- dict "realm" $import.name "enabled" true "clients" $clients | toPrettyJson -}}
{{- end -}}
