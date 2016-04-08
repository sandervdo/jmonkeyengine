package com.jme3.terrain.geomipmap;

import java.util.HashMap;

import com.jme3.bounding.BoundingBox;
import com.jme3.scene.Spatial;
import com.jme3.util.SafeArrayList;

public class TerrainNeighbours {
	
    protected static UpdatedTerrainPatch setTerrainPatch(TerrainPatch tp, HashMap<String,UpdatedTerrainPatch> updated) {
        UpdatedTerrainPatch utpRet = updated.get(tp.getName());
        if (utpRet == null) {
            utpRet = new UpdatedTerrainPatch(tp);
            updated.put(utpRet.getName(), utpRet);
            utpRet.setNewLod(tp.lod);
        }
    	return utpRet;
    }
    
    protected static void patchNeighbours(TerrainPatch patch, TerrainQuad terrainQuad) {
        if (!patch.searchedForNeighboursAlready) {
            // set the references to the neighbours
            patch.rightNeighbour = findRightPatch(patch, terrainQuad);
            patch.bottomNeighbour = findDownPatch(patch, terrainQuad);
            patch.leftNeighbour = findLeftPatch(patch, terrainQuad);
            patch.topNeighbour = findTopPatch(patch, terrainQuad);
            patch.searchedForNeighboursAlready = true;
        }
    }
    
    public synchronized static void findNeighboursLod(HashMap<String,UpdatedTerrainPatch> updated, TerrainQuad terrainQuad) {
        if (terrainQuad.getChildren() != null) {
            for (int x = terrainQuad.getChildren().size(); --x >= 0;) {
                Spatial child = terrainQuad.getChildren().get(x);
                if (child instanceof TerrainQuad) {
                    ((TerrainQuad) child).findNeighboursLod(updated);
                } else if (child instanceof TerrainPatch) {

                    TerrainPatch patch = (TerrainPatch) child;
                    patchNeighbours(patch, terrainQuad);
                    TerrainPatch right = patch.rightNeighbour;
                    TerrainPatch down = patch.bottomNeighbour;
                    TerrainPatch left = patch.leftNeighbour;
                    TerrainPatch top = patch.topNeighbour;

                    UpdatedTerrainPatch utp = updated.get(patch.getName());
                    if (utp == null) {
                        utp = new UpdatedTerrainPatch(patch, patch.lod);
                        updated.put(utp.getName(), utp);
                    }

                    if (right != null) {
                    	UpdatedTerrainPatch utpR = setTerrainPatch(right, updated);
                        utp.setRightLod(utpR.getNewLod());
                        utpR.setLeftLod(utp.getNewLod());
                    }
                    if (down != null) {
                        UpdatedTerrainPatch utpD = setTerrainPatch(down, updated);
                        utp.setBottomLod(utpD.getNewLod());
                        utpD.setTopLod(utp.getNewLod());
                    }
                    
                    if (left != null) {
                        UpdatedTerrainPatch utpL = setTerrainPatch(left, updated);
                        utp.setLeftLod(utpL.getNewLod());
                        utpL.setRightLod(utp.getNewLod());
                    }
                    if (top != null) {
                        UpdatedTerrainPatch utpT = setTerrainPatch(top, updated);
                        utp.setTopLod(utpT.getNewLod());
                        utpT.setBottomLod(utp.getNewLod());
                    }
                }
            }
        }
    }
    
    public synchronized static void fixEdges(HashMap<String,UpdatedTerrainPatch> updated, TerrainQuad terrainQuad) {
        if (terrainQuad.getChildren() != null) {
            for (int x = terrainQuad.getChildren().size(); --x >= 0;) {
                Spatial child = terrainQuad.getChildren().get(x);
                if (child instanceof TerrainQuad) {
                    ((TerrainQuad) child).fixEdges(updated);
                } else if (child instanceof TerrainPatch) {
                    TerrainPatch patch = (TerrainPatch) child;
                    UpdatedTerrainPatch utp = updated.get(patch.getName());

                    if(utp != null && utp.lodChanged()) {
                    	patchNeighbours(patch, terrainQuad);
                        TerrainPatch right = patch.rightNeighbour;
                        TerrainPatch down = patch.bottomNeighbour;
                        TerrainPatch top = patch.topNeighbour;
                        TerrainPatch left = patch.leftNeighbour;
                        
                        if (right != null) {
                            UpdatedTerrainPatch utpR = setTerrainPatch(right, updated);
                            utpR.setLeftLod(utp.getNewLod());
                            utpR.setFixEdges(true);
                        }
                        if (down != null) {
                            UpdatedTerrainPatch utpD = setTerrainPatch(down, updated);
                            utpD.setTopLod(utp.getNewLod());
                            utpD.setFixEdges(true);
                        }
                        if (top != null){
                            UpdatedTerrainPatch utpT = setTerrainPatch(top, updated);
                            utpT.setBottomLod(utp.getNewLod());
                            utpT.setFixEdges(true);
                        }
                        if (left != null){
                            UpdatedTerrainPatch utpL = setTerrainPatch(left, updated);
                            utpL.setRightLod(utp.getNewLod());
                            utpL.setFixEdges(true);
                        }
                    }
                }
            }
        }
    }
    
    protected static void fixNormalEdges(BoundingBox affectedArea, TerrainQuad terrainQuad) {
        if (terrainQuad.getChildren() == null)
            return;

        for (int x = terrainQuad.getChildren().size(); --x >= 0;) {
            Spatial child = terrainQuad.getChildren().get(x);
            if (child instanceof TerrainQuad) {
                if (affectedArea != null && affectedArea.intersects(((TerrainQuad) child).getWorldBound()) )
                    ((TerrainQuad) child).fixNormalEdges(affectedArea);
            } else if (child instanceof TerrainPatch) {
                if (affectedArea != null && !affectedArea.intersects(((TerrainPatch) child).getWorldBound()) ) // if doesn't intersect, continue
                    continue;

                TerrainPatch tp = (TerrainPatch) child;
                TerrainPatch right = findRightPatch(tp, terrainQuad);
                TerrainPatch bottom = findDownPatch(tp, terrainQuad);
                TerrainPatch top = findTopPatch(tp, terrainQuad);
                TerrainPatch left = findLeftPatch(tp, terrainQuad);
                TerrainPatch topLeft = null;
                if (top != null)
                    topLeft = findLeftPatch(top, terrainQuad);
                TerrainPatch bottomRight = null;
                if (right != null)
                    bottomRight = findDownPatch(right, terrainQuad);
                TerrainPatch topRight = null;
                if (top != null)
                    topRight = findRightPatch(top, terrainQuad);
                TerrainPatch bottomLeft = null;
                if (left != null)
                    bottomLeft = findDownPatch(left, terrainQuad);

                tp.fixNormalEdges(right, bottom, top, left, bottomRight, bottomLeft, topRight, topLeft);

            }
        } // for each child

    }
    
    protected static TerrainPatch getPatch(int quad, SafeArrayList<Spatial> children) {
        if (children != null)
            for (int x = children.size(); --x >= 0;) {
                Spatial child = children.get(x);
                if (child instanceof TerrainPatch) {
                    TerrainPatch tb = (TerrainPatch) child;
                    if (tb.getQuadrant() == quad)
                        return tb;
                }
            }
        return null;
    }
    
    protected static TerrainPatch findRightPatch(TerrainPatch tp, TerrainQuad terrainQuad) {
        if (tp.getQuadrant() == 1)
            return getPatch(3, terrainQuad.getChildren());
        else if (tp.getQuadrant() == 2)
            return getPatch(4, terrainQuad.getChildren());
        else if (tp.getQuadrant() == 3) {
            // find the patch to the right and ask it for child 1.
            TerrainQuad quad = findRightQuad(terrainQuad);
            if (quad != null)
                return quad.getPatch(1);
        } else if (tp.getQuadrant() == 4) {
            // find the patch to the right and ask it for child 2.
            TerrainQuad quad = findRightQuad(terrainQuad);
            if (quad != null)
                return quad.getPatch(2);
        }

        return null;
    }

    protected static TerrainPatch findDownPatch(TerrainPatch tp,  TerrainQuad terrainQuad) {
        if (tp.getQuadrant() == 1)
            return getPatch(2, terrainQuad.getChildren());
        else if (tp.getQuadrant() == 3)
            return getPatch(4, terrainQuad.getChildren());
        else if (tp.getQuadrant() == 2) {
            // find the patch below and ask it for child 1.
            TerrainQuad quad = findDownQuad(terrainQuad);
            if (quad != null)
                return quad.getPatch(1);
        } else if (tp.getQuadrant() == 4) {
            TerrainQuad quad = findDownQuad(terrainQuad);
            if (quad != null)
                return quad.getPatch(3);
        }

        return null;
    }


    protected static TerrainPatch findTopPatch(TerrainPatch tp,  TerrainQuad terrainQuad) {
        if (tp.getQuadrant() == 2)
            return getPatch(1, terrainQuad.getChildren());
        else if (tp.getQuadrant() == 4)
            return getPatch(3, terrainQuad.getChildren());
        else if (tp.getQuadrant() == 1) {
            // find the patch above and ask it for child 2.
            TerrainQuad quad = findTopQuad(terrainQuad);
            if (quad != null)
                return quad.getPatch(2);
        } else if (tp.getQuadrant() == 3) {
            TerrainQuad quad = findTopQuad(terrainQuad);
            if (quad != null)
                return quad.getPatch(4);
        }

        return null;
    }

    protected static TerrainPatch findLeftPatch(TerrainPatch tp,  TerrainQuad terrainQuad) {
        if (tp.getQuadrant() == 3)
            return getPatch(1, terrainQuad.getChildren());
        else if (tp.getQuadrant() == 4)
            return getPatch(2, terrainQuad.getChildren());
        else if (tp.getQuadrant() == 1) {
            // find the patch above and ask it for child 3.
            TerrainQuad quad = findLeftQuad(terrainQuad);
            if (quad != null)
                return quad.getPatch(3);
        } else if (tp.getQuadrant() == 2) {
            TerrainQuad quad = findLeftQuad(terrainQuad);
            if (quad != null)
                return quad.getPatch(4);
        }

        return null;
    }
    
    protected static TerrainQuad findRightQuad(TerrainQuad terrainQuad) {
        boolean useFinder = false;
        if (terrainQuad.getRemoteParent() == null || !(terrainQuad.getRemoteParent() instanceof TerrainQuad)) {
            if (terrainQuad.getNeighbourFinder() == null)
                return null;
            else
                useFinder = true;
        }

        TerrainQuad pQuad = null;
        if (!useFinder)
            pQuad = (TerrainQuad) terrainQuad.getRemoteParent();
        
        if (terrainQuad.getQuadrant() == 1)
            return pQuad.getQuad(3);
        else if (terrainQuad.getQuadrant() == 2)
            return pQuad.getQuad(4);
        else if (terrainQuad.getQuadrant() == 3) {
            TerrainQuad quad = pQuad.findRightQuad();
            if (quad != null)
                return quad.getQuad(1);
        } else if (terrainQuad.getQuadrant() == 4) {
            TerrainQuad quad = pQuad.findRightQuad();
            if (quad != null)
                return quad.getQuad(2);
        } else if (terrainQuad.getQuadrant() == 0) {
            // at the top quad
            if (useFinder) {
                TerrainQuad quad = terrainQuad.getNeighbourFinder().getRightQuad(terrainQuad);
                return quad;
            }
        }

        return null;
    }
    
    protected static TerrainQuad findDownQuad(TerrainQuad terrainQuad) {
        boolean useFinder = false;
        if (terrainQuad.getRemoteParent() == null || !(terrainQuad.getRemoteParent() instanceof TerrainQuad)) {
            if (terrainQuad.getNeighbourFinder() == null)
                return null;
            else
                useFinder = true;
        }

        TerrainQuad pQuad = null;
        if (!useFinder)
            pQuad = (TerrainQuad) terrainQuad.getRemoteParent();

        if (terrainQuad.getQuadrant() == 1)
            return pQuad.getQuad(2);
        else if (terrainQuad.getQuadrant() == 3)
            return pQuad.getQuad(4);
        else if (terrainQuad.getQuadrant() == 2) {
            TerrainQuad quad = pQuad.findDownQuad();
            if (quad != null)
                return quad.getQuad(1);
        } else if (terrainQuad.getQuadrant() == 4) {
            TerrainQuad quad = pQuad.findDownQuad();
            if (quad != null)
                return quad.getQuad(3);
        } else if (terrainQuad.getQuadrant() == 0) {
            // at the top quad
            if (useFinder) {
                TerrainQuad quad = terrainQuad.getNeighbourFinder().getDownQuad(terrainQuad);
                return quad;
            }
        }

        return null;
    }

    protected static TerrainQuad findTopQuad(TerrainQuad terrainQuad) {
        boolean useFinder = false;
        if (terrainQuad.getRemoteParent() == null || !(terrainQuad.getRemoteParent() instanceof TerrainQuad)) {
            if (terrainQuad.getNeighbourFinder() == null)
                return null;
            else
                useFinder = true;
        }

        TerrainQuad pQuad = null;
        if (!useFinder)
            pQuad = (TerrainQuad) terrainQuad.getRemoteParent();

        if (terrainQuad.getQuadrant() == 2)
            return pQuad.getQuad(1);
        else if (terrainQuad.getQuadrant() == 4)
            return pQuad.getQuad(3);
        else if (terrainQuad.getQuadrant() == 1) {
            TerrainQuad quad = pQuad.findTopQuad();
            if (quad != null)
                return quad.getQuad(2);
        } else if (terrainQuad.getQuadrant() == 3) {
            TerrainQuad quad = pQuad.findTopQuad();
            if (quad != null)
                return quad.getQuad(4);
        } else if (terrainQuad.getQuadrant() == 0) {
            // at the top quad
            if (useFinder) {
                TerrainQuad quad = terrainQuad.getNeighbourFinder().getTopQuad(terrainQuad);
                return quad;
            }
        }

        return null;
    }

    protected static TerrainQuad findLeftQuad(TerrainQuad terrainQuad) {
        boolean useFinder = false;
        if (terrainQuad.getRemoteParent() == null || !(terrainQuad.getRemoteParent() instanceof TerrainQuad)) {
            if (terrainQuad.getNeighbourFinder() == null)
                return null;
            else
                useFinder = true;
        }

        TerrainQuad pQuad = null;
        if (!useFinder)
            pQuad = (TerrainQuad) terrainQuad.getRemoteParent();

        if (terrainQuad.getQuadrant() == 3)
            return pQuad.getQuad(1);
        else if (terrainQuad.getQuadrant() == 4)
            return pQuad.getQuad(2);
        else if (terrainQuad.getQuadrant() == 1) {
            TerrainQuad quad = pQuad.findLeftQuad();
            if (quad != null)
                return quad.getQuad(3);
        } else if (terrainQuad.getQuadrant() == 2) {
            TerrainQuad quad = pQuad.findLeftQuad();
            if (quad != null)
                return quad.getQuad(4);
        } else if (terrainQuad.getQuadrant() == 0) {
            // at the top quad
            if (useFinder) {
                TerrainQuad quad = terrainQuad.getNeighbourFinder().getLeftQuad(terrainQuad);
                return quad;
            }
        }

        return null;
    }

}
