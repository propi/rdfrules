package com.github.propi.rdfrules.java.data;

import com.github.propi.rdfrules.java.DiscretizationTaskConverters;
import eu.easyminer.discretization.Support;
import eu.easyminer.discretization.task.EquidistanceDiscretizationTask;
import eu.easyminer.discretization.task.EquifrequencyDiscretizationTask;
import eu.easyminer.discretization.task.EquisizeDiscretizationTask;

/**
 * Created by Vaclav Zeman on 6. 8. 2018.
 */
abstract public class DiscretizationTask implements eu.easyminer.discretization.DiscretizationTask {

    abstract public com.github.propi.rdfrules.data.DiscretizationTask asScala();

    @Override
    public int hashCode() {
        return asScala().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        DiscretizationTask that = (DiscretizationTask) obj;
        return asScala().equals(that.asScala());
    }

    @Override
    public String toString() {
        return asScala().toString();
    }

    public enum Mode {
        INMEMORY(DiscretizationTaskConverters.inMemory()),
        EXTERNAL(DiscretizationTaskConverters.external());

        final private com.github.propi.rdfrules.data.DiscretizationTask.Mode mode;

        Mode(com.github.propi.rdfrules.data.DiscretizationTask.Mode mode) {
            this.mode = mode;
        }

        public com.github.propi.rdfrules.data.DiscretizationTask.Mode getMode() {
            return mode;
        }
    }

    public static class Equidistance extends DiscretizationTask implements EquidistanceDiscretizationTask {
        private com.github.propi.rdfrules.data.DiscretizationTask.Equidistance scala;

        public Equidistance(int bins) {
            this.scala = new com.github.propi.rdfrules.data.DiscretizationTask.Equidistance(bins);
        }

        @Override
        public com.github.propi.rdfrules.data.DiscretizationTask.Equidistance asScala() {
            return scala;
        }

        @Override
        public int getNumberOfBins() {
            return asScala().bins();
        }

        @Override
        public int getBufferSize() {
            return asScala().getBufferSize();
        }
    }

    public static class Equifrequency extends DiscretizationTask implements EquifrequencyDiscretizationTask {
        private com.github.propi.rdfrules.data.DiscretizationTask.Equifrequency scala;

        public Equifrequency(int bins, int buffer, Mode mode) {
            this.scala = new com.github.propi.rdfrules.data.DiscretizationTask.Equifrequency(bins, buffer, mode.getMode());
        }

        public Equifrequency(int bins, int buffer) {
            this.scala = DiscretizationTaskConverters.equifrequency(bins, buffer);
        }

        public Equifrequency(int bins) {
            this.scala = DiscretizationTaskConverters.equifrequency(bins);
        }

        @Override
        public com.github.propi.rdfrules.data.DiscretizationTask.Equifrequency asScala() {
            return scala;
        }

        @Override
        public int getNumberOfBins() {
            return asScala().bins();
        }

        @Override
        public int getBufferSize() {
            return asScala().getBufferSize();
        }
    }

    public static class Equisize extends DiscretizationTask implements EquisizeDiscretizationTask {
        private com.github.propi.rdfrules.data.DiscretizationTask.Equisize scala;

        public Equisize(double support, int buffer, Mode mode) {
            this.scala = new com.github.propi.rdfrules.data.DiscretizationTask.Equisize(support, buffer, mode.getMode());
        }

        public Equisize(double support, int buffer) {
            this.scala = DiscretizationTaskConverters.equisize(support, buffer);
        }

        public Equisize(double support) {
            this.scala = DiscretizationTaskConverters.equisize(support);
        }

        @Override
        public com.github.propi.rdfrules.data.DiscretizationTask.Equisize asScala() {
            return scala;
        }

        @Override
        public Support getMinSupport() {
            return asScala().getMinSupport();
        }

        @Override
        public int getBufferSize() {
            return asScala().getBufferSize();
        }
    }

}