tempdir := '$(mktemp -d)'

external-link-check:
    zola check

internal-link-check:
    zola build --output-dir {{tempdir}} --force --base-url {{tempdir}}
    linkchecker {{tempdir}}/index.html