package org.springframework.boot.test.mock.mockito;

import com.teketik.test.mockinbean.MockInBean;
import com.teketik.test.mockinbean.MockInBeans;
import com.teketik.test.mockinbean.SpyInBean;
import com.teketik.test.mockinbean.SpyInBeans;

import org.springframework.core.ResolvableType;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.TypeVariable;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>Similar to {@link DefinitionsParser} but handles {@link MockInBean} and {@link SpyInBean}.
 * <p>Every mock/spy {@link Definition} maps to one or more {@link InBeanDefinition}.
 * @see DefinitionsParser
 */
class InBeanDefinitionsParser {

    private final Map<DefinitionFacade, List<InBeanDefinition>> definitions = new HashMap<DefinitionFacade, List<InBeanDefinition>>();

    void parse(Class<?> source) {
        ReflectionUtils.doWithFields(source, (field) -> parseField(field, source));
    }

    private void parseField(Field element, Class<?> source) {
        for (MockInBean annotation: AnnotationUtils.getRepeatableAnnotations(element, MockInBean.class, MockInBeans.class)) {
            parseMockInBeanAnnotation(annotation, element, source);
        }
        for (SpyInBean annotation: AnnotationUtils.getRepeatableAnnotations(element, SpyInBean.class, SpyInBeans.class)) {
            parseSpyInBeanAnnotation(annotation, element, source);
        }
    }

    private void parseMockInBeanAnnotation(MockInBean annotation, Field field, Class<?> source) {
        final Set<ResolvableType> typesToMock = getOrDeduceTypes(field, source);
        Assert.state(!typesToMock.isEmpty(), () -> "Unable to deduce type to mock from " + field);
        for (ResolvableType typeToMock : typesToMock) {
            final MockDefinition mockDefinition = new MockDefinition(field.getName(), typeToMock, new Class[] {}, null, false, MockReset.AFTER, QualifierDefinition.forElement(field));
            InBeanDefinition inBeanDefinition = new InBeanDefinition(
                annotation.value(),
                StringUtils.isEmpty(annotation.name()) ? null : annotation.name()
            );
            addDefinition(mockDefinition, inBeanDefinition);
        }
    }

    private void parseSpyInBeanAnnotation(SpyInBean annotation, Field field, Class<?> source) {
        final Set<ResolvableType> typesToSpy = getOrDeduceTypes(field, source);
        Assert.state(!typesToSpy.isEmpty(), () -> "Unable to deduce type to spy from " + field);
        for (ResolvableType typeToSpy : typesToSpy) {
            final SpyDefinition spyDefinition = new SpyDefinition(field.getName(), typeToSpy, MockReset.AFTER, true, QualifierDefinition.forElement(field));
            InBeanDefinition inBeanDefinition = new InBeanDefinition(
                annotation.value(),
                StringUtils.isEmpty(annotation.name()) ? null : annotation.name()
            );
            addDefinition(spyDefinition, inBeanDefinition);
        }
    }

    private void addDefinition(Definition definition, InBeanDefinition inBeanDefinition) {
        addDefinition(new DefinitionFacade(definition), inBeanDefinition);
    }

    private void addDefinition(DefinitionFacade definition, InBeanDefinition inBeanDefinition) {
        List<InBeanDefinition> inBeanBaseDefinitions = definitions.get(definition);
        if (inBeanBaseDefinitions == null) {
            inBeanBaseDefinitions = new LinkedList<InBeanDefinition>();
            definitions.put(definition, inBeanBaseDefinitions);
        }
        inBeanBaseDefinitions.add(inBeanDefinition);
    }

    private Set<ResolvableType> getOrDeduceTypes(AnnotatedElement element, Class<?> source) {
        Set<ResolvableType> types = new LinkedHashSet<>();
        if (types.isEmpty() && element instanceof Field) {
            Field field = (Field) element;
            types.add(
                (field.getGenericType() instanceof TypeVariable)
                    ? ResolvableType.forField(field, source)
                    : ResolvableType.forField(field)
            );
        }
        return types;
    }

    public Map<DefinitionFacade, List<InBeanDefinition>> getDefinitions() {
        return Collections.unmodifiableMap(definitions);
    }

}
