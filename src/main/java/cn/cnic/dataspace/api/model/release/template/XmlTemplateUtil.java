package cn.cnic.dataspace.api.model.release.template;

import cn.cnic.dataspace.api.exception.CommonException;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @Auther: wdd
 * @Date: 2021/03/23/14:34
 * @Description:
 */
@Slf4j
public class XmlTemplateUtil {

    /**
     * //Obtain the author information of the second step template, etc
     */
    private static List getTemplateBasicInfo(Element bookstore, Template template) {
        // Start traversing the second layer, totaling four layers
        Iterator storeit = bookstore.elementIterator();
        // Start loop
        while (storeit.hasNext()) {
            Element bookElement = (Element) storeit.next();
            // Obtain fixed name attribute
            Attribute name1 = bookElement.attribute("name");
            // Obtain value based on the value corresponding to name
            String value1 = name1.getValue();
            // Obtain fixed name attribute
            Attribute value = bookElement.attribute("value");
            // Obtain value based on the value corresponding to name
            if ("name".equals(value1)) {
                template.setTemplateName(value.getValue());
            } else if ("desc".equals(value1)) {
                template.setTemplateDesc(value.getValue());
            } else if ("version".equals(value1)) {
                template.setVersion(value.getValue());
            } else if ("author".equals(value1)) {
                template.setTemplateAuthor(value.getValue());
            } else if ("root".equals(value1)) {
                return bookElement.elements("group");
            }
        }
        log.info("获取模板的基本信息成功..." + template.getTemplateAuthor());
        return null;
    }

    // Parsing templates
    public static Template getTemplate(String url) {
        Template template = new Template();
        SAXReader reader = new SAXReader();
        try {
            Document document = reader.read(url);
            Element element = document.getRootElement();
            List<Template.Group> groupList = new ArrayList<>();
            // Obtain basic information about templates
            List<Element> templateBasicInfo = getTemplateBasicInfo(element, template);
            for (Element elemens : templateBasicInfo) {
                Template.Group groupObj = new Template.Group();
                // Obtain fixed group attribute names
                Attribute value = elemens.attribute("value");
                groupObj.setName(value.getValue());
                // Obtain fixed group attribute descriptions
                Attribute desc = elemens.attribute("desc");
                groupObj.setDesc(desc.getValue());
                List<Template.Resource> listResource = new ArrayList<>();
                // Start processing the list in the group
                List<Element> lists = elemens.elements("list");
                for (Element list : lists) {
                    List<Element> beans = list.elements("bean");
                    // Start processing properties within the bean
                    for (Element bean : beans) {
                        List<Element> elements = bean.elements();
                        Template.Resource resource = new Template.Resource();
                        for (Element elementBean : elements) {
                            // Starting to set attributes to objects
                            setAttribute(resource, elementBean);
                        }
                        // Place each attribute content in the list
                        listResource.add(resource);
                    }
                    // Place the attribute sets within each group in the group
                    groupObj.setResources(listResource);
                }
                // How many groups are there in the list
                groupList.add(groupObj);
                template.setGroup(groupList);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("解析xml异常" + e);
            throw new CommonException(-1, "元数据标准模板文件解析异常");
        }
        return template;
    }

    /**
     * Set Options in Properties
     */
    private static List<Template.Options> setOptions(Element element) {
        Iterator listIterator = element.elementIterator();
        List<Template.Options> listOptions = new ArrayList<>();
        while (listIterator.hasNext()) {
            Element options = (Element) listIterator.next();
            List<Element> optionElements = options.elements();
            Template.Options optionObj = new Template.Options();
            for (Element optionElement : optionElements) {
                // Obtain fixed name attribute
                Attribute name1 = optionElement.attribute("name");
                // Obtain value based on the value corresponding to name
                String nameValue1 = name1.getValue();
                // Obtain fixed name attribute
                Attribute value1 = optionElement.attribute("value");
                // Obtain value based on the value corresponding to name
                if ("name".equals(nameValue1)) {
                    optionObj.setName(value1.getValue());
                } else if ("title".equals(nameValue1)) {
                    optionObj.setTitle(value1.getValue());
                } else if ("type".equals(nameValue1)) {
                    optionObj.setType(value1.getValue());
                } else if ("formate".equals(nameValue1)) {
                    optionObj.setFormate(value1.getValue());
                } else if ("isAll".equals(nameValue1)) {
                    optionObj.setIsAll(value1.getValue());
                } else if ("url".equals(nameValue1)) {
                    optionObj.setUrl(value1.getValue());
                } else if ("placeholder".equals(nameValue1)) {
                    optionObj.setPlaceholder(value1.getValue());
                } else if ("mode".equals(nameValue1)) {
                    optionObj.setMode(value1.getValue());
                } else if ("children".equals(nameValue1)) {
                    // To handle the children in the option
                    Element childrenElement = optionElement.element("list");
                    // Return the content of children
                    List<Template.Children> children = setChildren(childrenElement);
                    optionObj.setChildren(children);
                }
            }
            listOptions.add(optionObj);
        }
        return listOptions;
    }

    /**
     * Set Children in Properties
     */
    private static List<Template.Children> setChildren(Element element) {
        Iterator childrenIterator = element.elementIterator();
        List<Template.Children> listChildren = new ArrayList<>();
        while (childrenIterator.hasNext()) {
            Element Elementchildren = (Element) childrenIterator.next();
            List<Element> childrenList = Elementchildren.elements();
            Template.Children childrenObj = new Template.Children();
            for (Element children : childrenList) {
                // Obtain fixed name attribute
                Attribute childrenName = children.attribute("name");
                // Obtain value based on the value corresponding to name
                String childrenValueStr = childrenName.getValue();
                // Obtain fixed name attribute
                Attribute childrenValue = children.attribute("value");
                // Obtain value based on the value corresponding to name
                if ("name".equals(childrenValueStr)) {
                    childrenObj.setName(childrenValue.getValue());
                } else if ("title".equals(childrenValueStr)) {
                    childrenObj.setTitle(childrenValue.getValue());
                } else if ("type".equals(childrenValueStr)) {
                    childrenObj.setType(childrenValue.getValue());
                } else if ("multiply".equals(childrenValueStr)) {
                    childrenObj.setMultiply(childrenValue.getValue());
                } else if ("url".equals(childrenValueStr)) {
                    childrenObj.setUrl(childrenValue.getValue());
                } else if ("isAll".equals(childrenValueStr)) {
                    childrenObj.setIsAll(childrenValue.getValue());
                } else if ("formate".equals(childrenValueStr)) {
                    childrenObj.setFormate(childrenValue.getValue());
                } else if ("placeholder".equals(childrenValueStr)) {
                    childrenObj.setPlaceholder(childrenValue.getValue());
                } else if ("options".equals(childrenValueStr)) {
                    Element options = children.element("list");
                    // If there are options, continue to process the children inside
                    List<Template.Options> optionsList = setOptions(options);
                    childrenObj.setOptions(optionsList);
                }
            }
            listChildren.add(childrenObj);
        }
        return listChildren;
    }

    /**
     * Set Property Content
     */
    private static void setAttribute(Template.Resource resource, Element element) {
        Attribute nameValues = element.attribute("name");
        String nameValue = nameValues.getValue();
        Attribute beanValue = element.attribute("value");
        if ("name".equals(nameValue)) {
            resource.setName(beanValue.getValue());
        } else if ("title".equals(nameValue)) {
            resource.setTitle(beanValue.getValue());
        } else if ("type".equals(nameValue)) {
            resource.setType(beanValue.getValue());
        } else if ("check".equals(nameValue)) {
            resource.setCheck(beanValue.getValue());
        } else if ("multiply".equals(nameValue)) {
            resource.setMultiply(beanValue.getValue());
        } else if ("placeholder".equals(nameValue)) {
            resource.setPlaceholder(beanValue.getValue());
        } else if ("isAll".equals(nameValue)) {
            resource.setIsAll(beanValue.getValue());
        } else if ("iri".equals(nameValue)) {
            resource.setIri(beanValue.getValue());
        } else if ("url".equals(nameValue)) {
            resource.setUrl(beanValue.getValue());
        } else if ("language".equals(nameValue)) {
            resource.setLanguage(beanValue.getValue());
        } else if ("formate".equals(nameValue)) {
            resource.setFormate(beanValue.getValue());
        } else if ("mode".equals(nameValue)) {
            resource.setMode(beanValue.getValue());
        } else if ("options".equals(nameValue)) {
            // Processing options
            Element options = element.element("list");
            List<Template.Options> optionsList = setOptions(options);
            resource.setOptions(optionsList);
        } else if ("show".equals(nameValue)) {
            // Processing options
            Element options = element.element("list");
            List<Template.Options> optionsList = setOptions(options);
            resource.setShow(optionsList);
        } else if ("operation".equals(nameValue)) {
            // Processing options
            Element options = element.element("list");
            List<Template.Options> optionsList = setOptions(options);
            resource.setOperation(optionsList);
        }
    }
}
