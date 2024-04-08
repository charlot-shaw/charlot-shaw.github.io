{
    description = "Sparrows.dev static site builder.";

    inputs = {
        nixpkgs.url = "github:NixOS/nixpkgs";

        flake-utils = {
            url = "github:numtide/flake-utils";
        };

        bulma = {
            url = "github:jgthms/bulma/main";
            flake = false;
        };

    };

    outputs = {self,nixpkgs, flake-utils, bulma, ...}:

        flake-utils.lib.eachDefaultSystem (system: 
            let 
                pkgs = nixpkgs.legacyPackages.${system};
            in {
            packages = rec {
                default = sparrowsSite;

                sparrowsSite = pkgs.stdenv.mkDerivation rec {
                    pname = "sparrows.dev-site";
                    version = "0.0.1";
                    src = ./.;

                    buildInputs = with pkgs; [
                        zola
                        html-minifier
                    ];

                    # TODO make the flake input of Bulma work.

                    buildPhase = ''
                      zola build --output-dir $out

                      html-minifier --collapse-whitespace --preserve-line-breaks \
                                    --remove-comments \
                                    --minify-css true --remove-redundant-attributes \
                                    --remove-script-type-attributes \
                                    --minify-urls String \
                                    --input-dir $out/ --output-dir $out/ --file-ext html
                    '';

                    # Check links, homepage and CNAME file
                    checkPhase = ''
                    zola check

                    test -f $out/CNAME"
                    test -f $out/index.html"

                    '';
                };
            };
        }
        );
}