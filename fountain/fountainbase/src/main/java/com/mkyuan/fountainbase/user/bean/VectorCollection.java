package com.mkyuan.fountainbase.user.bean;

public class VectorCollection {
    private Vectors vectors;

    public VectorCollection() {
    }

    public Vectors getVectors() {
        return vectors;
    }

    public void setVectors(Vectors vectors) {
        this.vectors = vectors;
    }

    public static class Vectors {
        private int size;
        private String distance;

        public Vectors() {
        }

        public int getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public String getDistance() {
            return distance;
        }

        public void setDistance(String distance) {
            this.distance = distance;
        }

        @Override
        public String toString() {
            return "Vectors{" +
                    "size=" + size +
                    ", distance='" + distance + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "VectorCollection{" +
                "vectors=" + vectors +
                '}';
    }
}
