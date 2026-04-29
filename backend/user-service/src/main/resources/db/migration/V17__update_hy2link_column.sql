ALTER TABLE vpn_configurations ADD COLUMN hy2_links_json JSONB DEFAULT '[]'::jsonb;

UPDATE vpn_configurations
SET hy2_links_json = jsonb_build_array(hy2_link)
WHERE hy2_link IS NOT NULL AND hy2_link != '';

ALTER TABLE vpn_configurations DROP COLUMN hy2_link;

ALTER TABLE vpn_configurations ALTER COLUMN hy2_links_json SET NOT NULL;