package Util;


import java.util.HashMap;
import java.util.Map;

public class FragmentManager {
    public enum Etat { EN_ATTENTE, EN_COURS, TELECHARGE, NON_TELECHARGE }

    private Map<Integer, Fragment> fragments;

    public FragmentManager(int totalFragments) {
        fragments = new HashMap<>();
        for (int i = 0; i < totalFragments; i++) {
            fragments.put(i, new Fragment(i));
        }
    }

    public synchronized void mettreAJourEtat(int fragmentId, Etat etat, String source) {
        Fragment fragment = fragments.get(fragmentId);
        if (fragment != null) {
            fragment.setEtat(etat);
            fragment.setSource(source);
        }
    }

    public synchronized Fragment obtenirFragmentNonTelecharge() {
        for (Fragment fragment : fragments.values()) {
            if (fragment.getEtat() == Etat.NON_TELECHARGE) {
                return fragment;
            }
        }
        return null;
    }

    public synchronized Map<Integer, Fragment> obtenirFragments() {
        return new HashMap<>(fragments);
    }

    public static class Fragment {
        private final int id;
        private Etat etat;
        private String source;

        public Fragment(int id) {
            this.id = id;
            this.etat = Etat.EN_ATTENTE;
            this.source = null;
        }

        public int getId() {
            return id;
        }

        public Etat getEtat() {
            return etat;
        }

        public void setEtat(Etat etat) {
            this.etat = etat;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }
}
