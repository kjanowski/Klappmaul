/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.kmj.robots.klappmaul.blackBoxAPI.body;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Affine;
import javafx.scene.transform.MatrixType;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 *
 * @author Kathrin
 */
public class ColladaImporter {
    private static final Logger cLogger = Logger.getLogger(ColladaImporter.class.getName());
    
    TreeMap<String, PhongMaterial> mMaterials;
    TreeMap<String, MeshView> mMeshes;
    
    public ColladaImporter()
    {
        mMaterials = new TreeMap<>();
        mMeshes = new TreeMap<>();
    }
    
    public boolean loadFile(String path)
    {
        File file = new File(path);
        return loadFile(file);
    }
    
    public boolean loadFile(File file)
    {
        if(!file.exists())
            return false;
        
        try{
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document xml = builder.parse(file);

            //------------------------------------------------------------------

            Element root = xml.getDocumentElement();
            if(root != null)
            {
                parseDAE(root);
                return true;
            }
            

            return true;
        }catch (ParserConfigurationException e)
        {
            cLogger.log(Level.SEVERE, "could not parse dae file: {0}", e.toString());
        }
        catch(SAXException e)
        {
            cLogger.log(Level.SEVERE, "could not parse dae file: {0}", e.toString());
        }
        catch(IOException e){
            cLogger.log(Level.SEVERE, "could not load dae file: {0}", e.toString());
        }
        
        return false;
    }
    
    
    public MeshView getMesh(String name)
    {
        return mMeshes.get(name);
    }
    
    protected void parseDAE(Element root)
    {

        Element effectLib = getFirstChildElement(root, "library_effects");
        if(effectLib!=null)
        {
            NodeList effectMap = effectLib.getElementsByTagName("effect");
            for(int i=0; i<effectMap.getLength(); i++)
            {
                try{
                    parseEffect((Element)effectMap.item(i)); 
                }
                catch(Exception e)
                {
                    cLogger.warning("could not parse effect #"+i+": "+e.toString());
                }
            }            
        }


        Element geoLib = getFirstChildElement(root, "library_geometries");
        if(geoLib!=null)
        {
            NodeList geoMap = geoLib.getElementsByTagName("geometry");
            for(int i=0; i<geoMap.getLength(); i++)
            {
                try{
                    parseMesh((Element)geoMap.item(i)); 
                }
                catch(Exception e)
                {
                    cLogger.warning("could not parse mesh #"+i+": "+e.toString());
                }
            }            
        }
        
        Element sceneLib = getFirstChildElement(root, "library_visual_scenes");
        if(sceneLib!=null)
        {
            Element sceneElem = getFirstChildElement(sceneLib, "visual_scene");
            
            NodeList sceneNodeMap = sceneElem.getElementsByTagName("node");
            for(int i=0; i<sceneNodeMap.getLength(); i++)
            {
                try{
                    parseTransform((Element)sceneNodeMap.item(i));
                }
                catch(Exception e)
                {
                    cLogger.warning("could not parse transform for scene node #"+i+": "+e.getMessage());
                }
            }
        }
    }
    
    
    protected void parseEffect(Element effectElem)
    {
        String name=effectElem.getAttribute("id");
        
        //get the phong material node
        Element profileElem = getFirstChildElement(effectElem, "profile_COMMON");
        Element techElem = getFirstChildElement(profileElem, "technique");
        Element phongElem = getFirstChildElement(techElem, "phong");
        
        //get the diffuse color
        Element diffuseElem = getFirstChildElement(phongElem, "diffuse");
        Element diffColorElem = getFirstChildElement(diffuseElem, "color");
        
        Color color;
        if(diffColorElem!=null){
            StringTokenizer diffuseTokenizer= new StringTokenizer(diffColorElem.getTextContent());
            double r, g, b, a;
            
            r= Double.parseDouble(diffuseTokenizer.nextToken());
            g= Double.parseDouble(diffuseTokenizer.nextToken());
            b= Double.parseDouble(diffuseTokenizer.nextToken());
            a= Double.parseDouble(diffuseTokenizer.nextToken());
            color=new Color(r,g,b,a);
        }else color = Color.WHITE;
        
        PhongMaterial material = new PhongMaterial(color);
        mMaterials.put(name, material);
    }
    
    
    protected void parseMesh(Element geoElem)
    {
        //for simplicity, assume there is only one mesh
        Element meshElem = getFirstChildElement(geoElem, "mesh");
        
        MeshView meshView = new MeshView();
        TriangleMesh mesh = new TriangleMesh();
        meshView.setMesh(mesh);
        meshView.setMaterial(new PhongMaterial());
        
        String name=geoElem.getAttribute("name");
        
        //parse the verteces ---------------------------------------------------
        Element positionsElem = getChildElement(meshElem, "source", name+"-mesh-positions");
        Element vertecesElem = getFirstChildElement(positionsElem, "float_array");
        
        int coordCount = Integer.parseInt(vertecesElem.getAttribute("count"));
        int vertexCount = coordCount/3;
        float[] vertexCoords = new float[coordCount];
        
        StringTokenizer vertexTokenizer = new StringTokenizer(vertecesElem.getTextContent());
        int i=0;
        while(vertexTokenizer.hasMoreElements())
        {
            String coord = vertexTokenizer.nextToken();
            vertexCoords[i]= Float.parseFloat(coord);
            i++;
        }
        
        mesh.getPoints().addAll(vertexCoords, 0, coordCount);
        
        //parse the uv coordinates ---------------------------------------------
        Element uvElem = getChildElement(meshElem, "source", name+"-mesh-map-0");
        float[] uvCoords = null;

        if(uvElem != null)
        {
            Element uvFloatsElem = getFirstChildElement(uvElem, "float_array");

            int uvCoordCount = Integer.parseInt(uvFloatsElem.getAttribute("count"));
            uvCoords = new float[uvCoordCount];
            StringTokenizer uvTokenizer = new StringTokenizer(uvFloatsElem.getTextContent());
            i=0;
            while(uvTokenizer.hasMoreElements())
            {
                String coordU = uvTokenizer.nextToken();
                uvCoords[i]= Float.parseFloat(coordU);
                i++;
                
                String coordV = uvTokenizer.nextToken();
                uvCoords[i]= 1.0f - Float.parseFloat(coordV);
                i++;
            }
        }
        else
        {
            uvCoords=new float[]{0f, 0f};
        }
        mesh.getTexCoords().addAll(uvCoords, 0, uvCoords.length);
        
        
        //parse the normals ----------------------------------------------------
        Element normalsElem = getChildElement(meshElem, "source", name+"-mesh-normals");
        Element normalsFloatsElem = getFirstChildElement(normalsElem, "float_array");
        
        int normCoordCount = Integer.parseInt(normalsFloatsElem.getAttribute("count"));
        float[] normCoords = new float[normCoordCount];
        
        StringTokenizer normTokenizer = new StringTokenizer(normalsFloatsElem.getTextContent());
        i=0;
        while(normTokenizer.hasMoreElements())
        {
            String coord = normTokenizer.nextToken();
            normCoords[i]= Float.parseFloat(coord);
            i++;
        }
        
        mesh.getNormals().addAll(normCoords, 0, normCoordCount);
        
        //parse the faces ------------------------------------------------------
        Element trisElem = getFirstChildElement(meshElem, "triangles");
        Element trisFloatsElem = getFirstChildElement(trisElem, "p");
                
        int trisCount = Integer.parseInt(trisElem.getAttribute("count"));
        int trisAttrCount=trisCount*6; //#triangles * (3 verteces + 3 tex coords)
        int[] trisAttrs = new int[trisAttrCount];
        
        StringTokenizer trisTokenizer = new StringTokenizer(trisFloatsElem.getTextContent());
        i=0;
        while((i<trisAttrCount) && trisTokenizer.hasMoreElements())
        {
            //parse point index
            String token = trisTokenizer.nextToken();
            trisAttrs[i]= Integer.parseInt(token);
            i++;
            
            //ignore normals index
            token = trisTokenizer.nextToken();
            
            //parse UV index, but only if there are UVs
            if(uvElem != null)
            {
                token = trisTokenizer.nextToken();
                trisAttrs[i]= Integer.parseInt(token);
                i++;
            }
            else{
                trisAttrs[i]=0;
                i++;
            }
        }
        
        mesh.getFaces().addAll(trisAttrs, 0, trisAttrCount);
        
        //get the material
        String matName=trisElem.getAttribute("material");
        if(matName!=null)
        {
            matName=matName.replace("material", "effect");
            PhongMaterial material = mMaterials.get(matName);
            
            meshView.setMaterial(material);
        }
        
        mMeshes.put(name, meshView);
    }
    
    protected void parseTransform(Element sceneNode)
    {
        String name=sceneNode.getAttribute("name");
        MeshView meshView = mMeshes.get(name);
        
        if(meshView == null)
            return;
        
        
        Element matrixNode = getFirstChildElement(sceneNode, "matrix");
        double matrixVals[] = new double[16];
        
        StringTokenizer matrixTokenizer= new StringTokenizer(matrixNode.getTextContent());
        int i=0;
        while(matrixTokenizer.hasMoreTokens())
        {
            matrixVals[i]=Double.parseDouble(matrixTokenizer.nextToken());
            i++;
        }
        
        Affine aff = new Affine(matrixVals, MatrixType.MT_3D_4x4, 0);
        //aff.appendRotation(Math.PI, 0, 0, 0, new Point3D(1,0,0));
        meshView.getTransforms().add(aff);
    }
    
    
    
    
    
    
    protected Element getFirstChildElement(Element parent, String tagName)
    {
        NodeList childMap = parent.getElementsByTagName(tagName);
        if(childMap.getLength()==0)
            return null;
        
        return (Element)childMap.item(0);
    }

    protected Element getChildElement(Element parent, String tagName, String id)
    {
        NodeList childMap = parent.getElementsByTagName(tagName);
        if(childMap.getLength()==0)
            return null;
        
        int i=0;
        while(i<childMap.getLength())
        {
            Element child = (Element)childMap.item(i);
            if(child.getAttribute("id").equals(id))
                return child;
            else i++;
        }
        
        return null;
    }

}
