package xorinal;

import arc.Events;
import arc.graphics.Color;
import arc.util.Log;
import mindustry.Vars;
import mindustry.ctype.ContentType;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.graphics.g3d.GenericMesh;
import mindustry.graphics.g3d.HexMesh;
import mindustry.graphics.g3d.HexSkyMesh;
import mindustry.graphics.g3d.MultiMesh;
import mindustry.graphics.g3d.NoiseMesh;
import mindustry.type.Planet;

public class PlanetMeshes {
    public static void init() {
        Events.on(ClientLoadEvent.class, e -> setup());
    }

    private static void setup() {
        try {
            Planet tetra = Vars.content.getByName(ContentType.planet, "xorinal-tetra");
            Planet xorinal = Vars.content.getByName(ContentType.planet, "xorinal-xorinal");

            if (tetra != null) {
                tetra.meshLoader = () -> makeNoise(tetra, 7, 8, 7, 0.55f, 1.5f, 0.55f,
                    new String[]{"0e2440","1a3d63","265e85","4ea8d4","8ec8e8","b8dcef","e6f4ff","ffffff"});
                tetra.cloudMeshLoader = () -> clouds(tetra,
                    new CloudLayer(2, 0.20f, 0.205f, 6, "ffffff90", 3, 0.55f, 1.0f, 0.60f),
                    new CloudLayer(5, 0.12f, 0.225f, 6, "cde9ff70", 3, 0.6f, 1.5f, 0.62f),
                    new CloudLayer(11, 0.06f, 0.245f, 5, "e6f4ff40", 2, 0.5f, 2.2f, 0.7f));
            }
            if (xorinal != null) {
                xorinal.meshLoader = () -> makeNoise(xorinal, 13, 8, 7, 0.55f, 1.5f, 0.6f,
                    new String[]{"0c1f10","17371a","244a26","3E7B2F","5fa840","8ec860","b6dc7c","e0f0a8"});
                xorinal.cloudMeshLoader = () -> clouds(xorinal,
                    new CloudLayer(4, 0.18f, 0.205f, 6, "9fd47880", 3, 0.55f, 1.1f, 0.58f),
                    new CloudLayer(9, 0.10f, 0.225f, 6, "c8e6a060", 3, 0.6f, 1.6f, 0.62f),
                    new CloudLayer(17, 0.05f, 0.245f, 5, "e0f0a830", 2, 0.5f, 2.3f, 0.7f));
            }
        } catch (Exception e) {
            Log.err("[Xorinal] planet mesh setup failed: " + e);
            try {
                Planet tetra = Vars.content.getByName(ContentType.planet, "xorinal-tetra");
                Planet xorinal = Vars.content.getByName(ContentType.planet, "xorinal-xorinal");
                if (tetra != null) tetra.meshLoader = () -> new HexMesh(tetra, 6);
                if (xorinal != null) xorinal.meshLoader = () -> new HexMesh(xorinal, 6);
            } catch (Exception e2) {}
        }
    }

    private static GenericMesh makeNoise(Planet planet, int seed, int divisions, int octaves, float persistence, float scale, float mag, String[] hex) {
        Color[] c = new Color[8];
        for (int i = 0; i < 8; i++) {
            String h = i < hex.length ? hex[i] : hex[hex.length - 1];
            c[i] = Color.valueOf(h);
        }
        // The 8-color NoiseMesh constructor exists in Mindustry's runtime jar but isn't in the
        // classpath jar we compile against — call it via reflection so we get gradient terrain.
        try {
            for (var ctor : NoiseMesh.class.getConstructors()) {
                if (ctor.getParameterCount() == 16) {
                    return (GenericMesh) ctor.newInstance(
                        planet, seed, divisions, 0.6f, octaves, persistence, scale, mag,
                        c[0], c[1], c[2], c[3], c[4], c[5], c[6], c[7]);
                }
            }
        } catch (Exception ex) {
            Log.err("[Xorinal] reflective NoiseMesh failed: " + ex);
        }
        return new NoiseMesh(planet, seed, divisions, c[3], 0.6f, octaves, persistence, scale, mag);
    }

    private static class CloudLayer {
        final int seed; final float speed, radius; final int divisions; final String color;
        final int octaves; final float persistence, scale, threshold;
        CloudLayer(int seed, float speed, float radius, int divisions, String color, int octaves, float persistence, float scale, float threshold) {
            this.seed = seed; this.speed = speed; this.radius = radius; this.divisions = divisions;
            this.color = color; this.octaves = octaves; this.persistence = persistence;
            this.scale = scale; this.threshold = threshold;
        }
    }

    private static MultiMesh clouds(Planet planet, CloudLayer... layers) {
        GenericMesh[] meshes = new GenericMesh[layers.length];
        for (int i = 0; i < layers.length; i++) {
            CloudLayer L = layers[i];
            meshes[i] = new HexSkyMesh(planet, L.seed, L.speed, L.radius, L.divisions,
                Color.valueOf(L.color), L.octaves, L.persistence, L.scale, L.threshold);
        }
        return new MultiMesh(meshes);
    }
}
