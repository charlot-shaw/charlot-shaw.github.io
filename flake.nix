{
    description = "Sparrows.dev static site builder.";

    inputs = {
        nixpkgs.url = "github:NixOS/nixpkgs";

        flake-utils = {
            url = "github:numtide/flake-utils";
        };

    };

    outputs = {self,nixpkgs, flake-utils,...}:

        flake-utils.lib.eachDefaultSystem (system: 
            let 
                pkgs = nixpkgs.legacyPackages.${system};
            in {
            packages = {
                sparrowssite = pkgs.stdenv.mkDerivation rec {
                    default = true;
                    pname = "sparrows.dev-site";
                    version = "0.0.1";
                    src = ./.;

                    buildInputs = with pkgs; [
                        zola
                        just
                    ];

                    buildPhase = ''
                      zola build
                    '';

                    checkPhase = ''
                    zola check
                    '';

                    installPhase = ''
                    runHook preInstall
                    mkdir -p $out
                    cp -r public/ $out
                    runHook postInstall
                    '';
                };
            };
        }
        );
}